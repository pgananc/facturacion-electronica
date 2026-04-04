import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../domain/entities/company_establishment.dart';
import '../../../domain/repositories/company_repository.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../widgets/loading_widget.dart';
import '../../widgets/logout_action.dart';

const Color _kNavyPrimary = Color(0xFF001255);

class CompanySelectionPage extends StatefulWidget {
  const CompanySelectionPage({super.key});

  @override
  State<CompanySelectionPage> createState() => _CompanySelectionPageState();
}

class _CompanySelectionPageState extends State<CompanySelectionPage> {
  final Map<int, _CompanyInfo> _companyInfo = {};
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadCompanyInfo());
  }

  Future<void> _loadCompanyInfo() async {
    final auth = context.read<AuthBloc>().state;
    if (auth is! AuthAuthenticated) {
      setState(() => _loading = false);
      return;
    }
    final companies = auth.user.companyEstablishments;
    final repo = getIt<CompanyRepository>();
    final results = await Future.wait(
      companies.map((c) async {
        final nameResult = await repo.getCompanyDisplayName(c.idCompany);
        final name = nameResult.getOrElse(() => 'Compañía ${c.idCompany}');
        String? ruc;
        final rucResult = await repo.getCompanyIdentification(c.idCompany);
        rucResult.fold((_) {}, (v) => ruc = v);
        return MapEntry(
          c.idCompany,
          _CompanyInfo(
            name: name,
            ruc: ruc,
            establishmentCount: c.establishmentIds.length,
          ),
        );
      }),
    );
    if (!mounted) return;
    setState(() {
      for (final e in results) {
        _companyInfo[e.key] = e.value;
      }
      _loading = false;
    });
  }

  Future<void> _confirmAndSelect(CompanyEstablishment c, String displayName) async {
    final auth = context.read<AuthBloc>().state;
    if (auth is! AuthAuthenticated) return;

    if (auth.user.companyId == c.idCompany) {
      final storage = getIt<StorageService>();
      if (!storage.isEmissionSessionSelected) {
        context.push('/emission-point');
        return;
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Ya estás en esta compañía')),
      );
      return;
    }

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cambiar Compañía'),
        content: Text(
          '¿Cambiar a "$displayName"?\n\n'
          'Se actualizará tu sesión y necesitarás seleccionar '
          'un nuevo punto de emisión.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Cambiar'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    final storage = getIt<StorageService>();
    await storage.saveSelectedCompany('${c.idCompany}');

    final info = _companyInfo[c.idCompany];
    if (info?.ruc != null && info!.ruc!.isNotEmpty) {
      await storage.saveCompanyIdentification(info.ruc!);
    }

    await storage.clearEmissionSession();

    if (!mounted) return;
    context.read<AuthBloc>().add(
          SessionUserUpdatedEvent(
            auth.user.copyWith(
              companyId: c.idCompany,
              companyIdentification: info?.ruc ?? auth.user.companyIdentification,
            ),
          ),
        );
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Compañía cambiada a: $displayName'),
        backgroundColor: Theme.of(context).colorScheme.primary,
      ),
    );
    context.push('/emission-point');
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return Scaffold(
      backgroundColor: cs.surfaceContainerLowest,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const _CompanyBrandingHeader(),
          Expanded(
            child: BlocBuilder<AuthBloc, AuthState>(
              builder: (context, state) {
                if (state is! AuthAuthenticated) {
                  return const Center(child: Text('No autenticado'));
                }
                final list = state.user.companyEstablishments;
                if (_loading && list.isNotEmpty) {
                  return const Center(child: LoadingWidget());
                }
                if (list.isEmpty) {
                  return Center(
                    child: Padding(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Icons.business_outlined,
                            size: 64,
                            color: cs.onSurfaceVariant.withValues(alpha: 0.6),
                          ),
                          const SizedBox(height: 20),
                          Text(
                            'No hay compañías asociadas a su usuario.\n'
                            'Cierre sesión y vuelva a ingresar, o contacte a soporte.',
                            textAlign: TextAlign.center,
                            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                  color: cs.onSurfaceVariant,
                                ),
                          ),
                        ],
                      ),
                    ),
                  );
                }

                final active = state.user.companyId;

                return ListView.builder(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                  itemCount: list.length,
                  itemBuilder: (context, i) {
                    final c = list[i];
                    final info = _companyInfo[c.idCompany];
                    final name = info?.name ?? 'Compañía ${c.idCompany}';
                    final isActive = c.idCompany == active;
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _CompanyTile(
                        name: name,
                        ruc: info?.ruc,
                        establishmentCount: info?.establishmentCount ?? 0,
                        isActive: isActive,
                        onTap: () => _confirmAndSelect(c, name),
                      ),
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _CompanyInfo {
  const _CompanyInfo({required this.name, this.ruc, this.establishmentCount = 0});
  final String name;
  final String? ruc;
  final int establishmentCount;
}

class _CompanyBrandingHeader extends StatelessWidget {
  const _CompanyBrandingHeader();

  @override
  Widget build(BuildContext context) {
    return BlocSelector<AuthBloc, AuthState, int>(
      selector: (state) => state is AuthAuthenticated
          ? state.user.companyEstablishments.length
          : 0,
      builder: (context, companyCount) {
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
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      ColorFiltered(
                        colorFilter: const ColorFilter.mode(Colors.white, BlendMode.srcIn),
                        child: Image.asset(
                          'assets/images/logo-wallparisoft.png',
                          width: 52,
                          height: 52,
                        ),
                      ),
                      const Spacer(),
                      const IconTheme(
                        data: IconThemeData(color: Colors.white),
                        child: LogoutAction(),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Selecciona tu compañía',
                          style: Theme.of(context).textTheme.titleLarge?.copyWith(
                                color: Colors.white,
                                fontWeight: FontWeight.w800,
                                letterSpacing: 0.3,
                              ),
                        ),
                        const SizedBox(height: 6),
                        Text(
                          '$companyCount compañía${companyCount != 1 ? "s" : ""} disponible${companyCount != 1 ? "s" : ""}',
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
      },
    );
  }
}

class _CompanyTile extends StatelessWidget {
  const _CompanyTile({
    required this.name,
    this.ruc,
    required this.establishmentCount,
    required this.isActive,
    required this.onTap,
  });

  final String name;
  final String? ruc;
  final int establishmentCount;
  final bool isActive;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;
    const radius = 16.0;

    return Card(
      clipBehavior: Clip.antiAlias,
      color: isActive
          ? cs.primaryContainer.withValues(alpha: cs.brightness == Brightness.dark ? 0.35 : 0.55)
          : cs.surface,
      shadowColor: Colors.transparent,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(radius),
        side: isActive
            ? BorderSide(color: cs.primary, width: 2)
            : BorderSide(color: cs.outlineVariant.withValues(alpha: 0.35)),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(radius),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: isActive
                      ? cs.primary.withValues(alpha: 0.12)
                      : cs.surfaceContainerHighest,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: isActive
                        ? cs.primary.withValues(alpha: 0.45)
                        : cs.outlineVariant.withValues(alpha: 0.4),
                  ),
                ),
                child: Icon(
                  Icons.business_rounded,
                  size: 24,
                  color: isActive ? cs.primary : cs.onSurfaceVariant,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      name,
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: isActive ? cs.onSurface : null,
                      ),
                    ),
                    if (ruc != null && ruc!.isNotEmpty) ...[
                      const SizedBox(height: 3),
                      Text(
                        'RUC: $ruc',
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: cs.onSurfaceVariant,
                        ),
                      ),
                    ],
                    const SizedBox(height: 4),
                    Text(
                      '$establishmentCount establecimiento${establishmentCount != 1 ? "s" : ""}',
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: cs.onSurfaceVariant,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ),
              if (isActive)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(
                    color: cs.primary,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    'Activa',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: cs.onPrimary,
                      letterSpacing: 0.2,
                    ),
                  ),
                )
              else
                Icon(
                  Icons.chevron_right_rounded,
                  color: cs.onSurfaceVariant,
                ),
            ],
          ),
        ),
      ),
    );
  }
}
