import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../domain/entities/emission_session.dart';
import '../../../domain/entities/invoice_issuance_point.dart';
import '../../../domain/entities/user.dart';
import '../../../domain/repositories/invoice_session_repository.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../widgets/loading_widget.dart';
import '../../widgets/logout_action.dart';

const Color _kNavyPrimary = Color(0xFF001255);

class EmissionPointPage extends StatefulWidget {
  const EmissionPointPage({super.key});

  @override
  State<EmissionPointPage> createState() => _EmissionPointPageState();
}

class _EmissionPointPageState extends State<EmissionPointPage> {
  final _sessionRepo = getIt<InvoiceSessionRepository>();

  List<InvoiceIssuancePoint> _points = [];
  bool _loading = true;
  String? _error;
  bool _assigning = false;
  int? _assigningPointId;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final authState = context.read<AuthBloc>().state;
      User? user;
      if (authState is AuthAuthenticated) {
        user = authState.user;
      }
      final storage = getIt<StorageService>();
      final idCompany = await resolveCompanyId(storage, user);
      if (!mounted) return;
      if (idCompany == null || user == null) {
        setState(() {
          _loading = false;
          _error = 'No hay compañía en la sesión.';
        });
        return;
      }
      final est = user.companyEstablishments
          .where((c) => c.idCompany == idCompany)
          .expand((c) => c.establishmentIds)
          .toList();
      if (est.isEmpty) {
        setState(() {
          _loading = false;
          _error = 'No hay establecimientos asignados para su usuario.';
        });
        return;
      }
      final result = await _sessionRepo.getAvailablePoints(est);
      if (!mounted) return;
      result.fold(
        (failure) => setState(() {
          _loading = false;
          _error = failure.message;
        }),
        (list) => setState(() {
          _points = list..sort((a, b) => a.code.compareTo(b.code));
          _loading = false;
        }),
      );
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'No se pudieron cargar los puntos de emisión.';
      });
    }
  }

  Future<void> _select(InvoiceIssuancePoint p) async {
    setState(() {
      _assigning = true;
      _assigningPointId = p.idInvoiceIssuancePoint;
    });
    try {
      final result = await _sessionRepo.assignPoint(p.idInvoiceIssuancePoint);
      if (result.isLeft()) {
        if (!mounted) return;
        final failure = result.fold((f) => f, (_) => null)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(failure.message), backgroundColor: Theme.of(context).colorScheme.error),
        );
      } else {
        final session = EmissionSession(
          idInvoiceIssuancePoint: p.idInvoiceIssuancePoint,
          idEstablishment: p.establishmentId,
          issuancePointCode: p.code,
          establishmentCode: '001',
        );
        await getIt<StorageService>().saveEmissionSession(session);
        if (!mounted) return;
        context.go('/dashboard');
      }
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('No se pudo asignar el punto de emisión'),
          backgroundColor: Theme.of(context).colorScheme.error,
        ),
      );
    } finally {
      if (mounted) {
        setState(() {
          _assigning = false;
          _assigningPointId = null;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final activeSession = getIt<StorageService>().getEmissionSession();
    return Scaffold(
      backgroundColor: cs.surfaceContainerLowest,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _EmissionPointBrandingHeader(
            onBack: () {
              if (context.canPop()) {
                context.pop();
              } else {
                context.go('/select-company');
              }
            },
            showLogout: true,
          ),
          Expanded(
            child: _loading
                ? const Center(child: LoadingWidget())
                : _error != null
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.error_outline_rounded, size: 56, color: cs.error),
                              const SizedBox(height: 16),
                              Text(
                                _error!,
                                textAlign: TextAlign.center,
                                style: Theme.of(context).textTheme.bodyLarge,
                              ),
                              const SizedBox(height: 24),
                              FilledButton(
                                onPressed: _load,
                                style: FilledButton.styleFrom(
                                  backgroundColor: _kNavyPrimary,
                                  foregroundColor: Colors.white,
                                  padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 14),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(14),
                                  ),
                                ),
                                child: const Text('Reintentar'),
                              ),
                            ],
                          ),
                        ),
                      )
                    : _points.isEmpty
                        ? Center(
                            child: Padding(
                              padding: const EdgeInsets.all(24),
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(
                                    Icons.inventory_2_outlined,
                                    size: 64,
                                    color: cs.onSurfaceVariant.withValues(alpha: 0.6),
                                  ),
                                  const SizedBox(height: 20),
                                  Text(
                                    'No hay puntos de emisión libres. Espere a que otro usuario libere un punto o contacte al administrador.',
                                    textAlign: TextAlign.center,
                                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                          color: cs.onSurfaceVariant,
                                        ),
                                  ),
                                ],
                              ),
                            ),
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                            itemCount: _points.length,
                            itemBuilder: (context, i) {
                              final p = _points[i];
                              final isActive = activeSession?.idInvoiceIssuancePoint == p.idInvoiceIssuancePoint ||
                                  _assigningPointId == p.idInvoiceIssuancePoint;
                              return Padding(
                                padding: const EdgeInsets.only(bottom: 12),
                                child: _EmissionPointCard(
                                  point: p,
                                  isActive: isActive,
                                  assigning: _assigning,
                                  onTap: _assigning ? null : () => _select(p),
                                ),
                              );
                            },
                          ),
          ),
        ],
      ),
    );
  }
}

class _EmissionPointBrandingHeader extends StatelessWidget {
  const _EmissionPointBrandingHeader({this.onBack, this.showLogout = false});

  final VoidCallback? onBack;
  final bool showLogout;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            _kNavyPrimary,
            _kNavyPrimary.withValues(alpha: 0.88),
          ],
        ),
        borderRadius: const BorderRadius.vertical(
          bottom: Radius.circular(28),
        ),
        boxShadow: [
          BoxShadow(
            color: _kNavyPrimary.withValues(alpha: 0.35),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(4, 0, 16, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  if (onBack != null)
                    IconButton(
                      onPressed: onBack,
                      icon: const Icon(Icons.arrow_back_ios_new_rounded, color: Colors.white),
                      tooltip: MaterialLocalizations.of(context).backButtonTooltip,
                    )
                  else
                    const SizedBox(width: 16),
                  ColorFiltered(
                    colorFilter: const ColorFilter.mode(Colors.white, BlendMode.srcIn),
                    child: Image.asset(
                      'assets/images/logo-wallparisoft.png',
                      width: 52,
                      height: 52,
                    ),
                  ),
                  const Spacer(),
                  if (showLogout)
                    const IconTheme(
                      data: IconThemeData(color: Colors.white),
                      child: LogoutAction(),
                    ),
                ],
              ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Punto de emisión',
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            color: Colors.white,
                            fontWeight: FontWeight.w800,
                            letterSpacing: 0.3,
                          ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Seleccione un punto disponible para continuar',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: Colors.white.withValues(alpha: 0.75),
                          ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _EmissionPointCard extends StatelessWidget {
  const _EmissionPointCard({
    required this.point,
    required this.isActive,
    required this.assigning,
    required this.onTap,
  });

  final InvoiceIssuancePoint point;
  final bool isActive;
  final bool assigning;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return Card(
      elevation: isActive ? 2 : 0,
      shadowColor: Colors.black.withValues(alpha: 0.12),
      color: isActive ? _kNavyPrimary.withValues(alpha: 0.08) : cs.surface,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: isActive ? _kNavyPrimary : cs.outlineVariant.withValues(alpha: 0.65),
          width: isActive ? 2 : 1,
        ),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: isActive ? _kNavyPrimary.withValues(alpha: 0.15) : cs.primaryContainer.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  Icons.badge_outlined,
                  color: isActive ? _kNavyPrimary : cs.onPrimaryContainer,
                  size: 26,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Punto ${point.code}',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w700,
                            color: isActive ? _kNavyPrimary : cs.onSurface,
                          ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Establecimiento: ${point.establishmentId}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: cs.onSurfaceVariant,
                          ),
                    ),
                  ],
                ),
              ),
              if (assigning && isActive)
                const SizedBox(
                  width: 28,
                  height: 28,
                  child: CircularProgressIndicator(strokeWidth: 2.5),
                )
              else
                Icon(
                  Icons.chevron_right_rounded,
                  color: isActive ? _kNavyPrimary : cs.onSurfaceVariant,
                ),
            ],
          ),
        ),
      ),
    );
  }
}
