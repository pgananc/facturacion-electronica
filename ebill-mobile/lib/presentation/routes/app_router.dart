import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/di/injection.dart';
import '../../core/storage/storage_service.dart';
import '../bloc/auth/auth_bloc.dart';
import '../pages/login/login_page.dart';
import '../pages/dashboard/dashboard_page.dart';
import '../pages/more/more_page.dart';
import '../pages/reports/pending_invoices_page.dart';
import '../pages/reports/reports_page.dart';
import '../pages/reports/sales_by_client_page.dart';
import '../pages/reports/sales_by_period_page.dart';
import '../pages/reports/sales_by_product_page.dart';
import '../pages/credit_notes/credit_note_detail_page.dart';
import '../pages/credit_notes/credit_note_form_page.dart';
import '../pages/credit_notes/credit_notes_page.dart';
import '../pages/invoices/invoices_page.dart';
import '../pages/invoices/invoice_detail_page.dart';
import '../pages/invoices/invoice_form_page.dart';
import '../pages/proformas/proformas_page.dart';
import '../pages/proformas/proforma_form_page.dart';
import '../pages/proformas/proforma_detail_page.dart';
import '../pages/referral_guides/referral_guide_detail_page.dart';
import '../pages/referral_guides/referral_guide_form_page.dart';
import '../pages/referral_guides/referral_guides_page.dart';
import '../pages/login/forgot_password_page.dart';
import '../pages/invoices/emission_point_page.dart';
import '../pages/invoices/drafts_page.dart';
import '../pages/customers/customers_page.dart';
import '../pages/customers/customer_form_page.dart';
import '../pages/customers/customer_detail_page.dart';
import '../pages/products/products_page.dart';
import '../pages/products/product_form_page.dart';
import '../pages/products/product_detail_page.dart';
import '../pages/profile/change_password_page.dart';
import '../pages/profile/edit_profile_page.dart';
import '../pages/profile/profile_page.dart';
import '../pages/settings/company_selection_page.dart';
import '../pages/settings/settings_page.dart';
import '../widgets/main_scaffold.dart';
import 'auth_refresh_notifier.dart';

int? _parseId(GoRouterState state) =>
    int.tryParse(state.pathParameters['id'] ?? '');

CupertinoPage<void> _buildCupertinoPage(GoRouterState state, Widget child) {
  return CupertinoPage<void>(
    key: state.pageKey,
    child: child,
  );
}

final _rootNavigatorKey = GlobalKey<NavigatorState>();
final _shellNavigatorKey = GlobalKey<NavigatorState>();

class AppRouter {
  static AuthRefreshNotifier? _refreshNotifier;
  static GoRouter? _router;

  static void resetRouterForTesting() {
    _refreshNotifier?.dispose();
    _refreshNotifier = null;
    _router = null;
  }

  static GoRouter get router {
    _router ??= _buildRouter();
    return _router!;
  }

  static GoRouter _buildRouter() {
    final authBloc = getIt<AuthBloc>();
    _refreshNotifier = AuthRefreshNotifier(authBloc);

    return GoRouter(
      navigatorKey: _rootNavigatorKey,
      initialLocation: '/login',
      refreshListenable: _refreshNotifier,
      redirect: (BuildContext context, GoRouterState state) {
        final loc = state.matchedLocation;
        final authState = authBloc.state;

        if (authState is AuthInitial || authState is AuthLoading) {
          return null;
        }

        const publicRoutes = ['/login', '/forgot-password'];

        if (authState is AuthAuthenticated) {
          final storage = getIt<StorageService>();

          if (publicRoutes.contains(loc)) {
            if (storage.isEmissionSessionSelected) return '/dashboard';
            return '/select-company';
          }
          if (!storage.isEmissionSessionSelected &&
              loc != '/emission-point' &&
              loc != '/select-company') {
            return '/select-company';
          }
          return null;
        }

        if (!publicRoutes.contains(loc)) {
          return '/login';
        }
        return null;
      },
      errorBuilder: (context, state) => Scaffold(
        appBar: AppBar(title: const Text('Página no encontrada')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.error_outline, size: 64, color: Theme.of(context).colorScheme.error),
              const SizedBox(height: 16),
              Text('Ruta no encontrada', style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 8),
              Text(
                kDebugMode
                    ? 'Ruta no encontrada: ${state.uri}'
                    : 'Página no encontrada',
                style: Theme.of(context).textTheme.bodyMedium,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 24),
              FilledButton(
                onPressed: () => GoRouter.of(context).go('/dashboard'),
                child: const Text('Ir al inicio'),
              ),
            ],
          ),
        ),
      ),
      routes: [
        GoRoute(
          path: '/login',
          name: 'login',
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const LoginPage()),
        ),
        GoRoute(
          path: '/forgot-password',
          name: 'forgot-password',
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const ForgotPasswordPage()),
        ),
        GoRoute(
          path: '/emission-point',
          name: 'emission-point',
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const EmissionPointPage()),
        ),

        ShellRoute(
          navigatorKey: _shellNavigatorKey,
          builder: (context, state, child) {
            final loc = state.matchedLocation;
            int idx = 0;
            if (loc.startsWith('/invoices')) {
              idx = 1;
            } else if (loc.startsWith('/customers')) {
              idx = 2;
            } else if (loc.startsWith('/products')) {
              idx = 3;
            } else if (loc == '/more') {
              idx = 4;
            }
            return MainScaffold(
              currentIndex: idx,
              onTap: (i) {
                switch (i) {
                  case 0:
                    context.go('/dashboard');
                  case 1:
                    context.go('/invoices');
                  case 2:
                    context.go('/customers');
                  case 3:
                    context.go('/products');
                  case 4:
                    context.go('/more');
                }
              },
              child: child,
            );
          },
          routes: [
            GoRoute(
              path: '/dashboard',
              name: 'dashboard',
              builder: (context, state) => const DashboardPage(),
            ),
            GoRoute(
              path: '/invoices',
              name: 'invoices',
              builder: (context, state) => const InvoicesPage(),
            ),
            GoRoute(
              path: '/customers',
              name: 'customers',
              builder: (context, state) => const CustomersPage(),
            ),
            GoRoute(
              path: '/products',
              name: 'products',
              builder: (context, state) => const ProductsPage(),
            ),
            GoRoute(
              path: '/more',
              name: 'more',
              builder: (context, state) => const MorePage(),
            ),
          ],
        ),

        // Detail & form routes — pushed on top (no bottom nav)
        GoRoute(
          path: '/invoices/new',
          name: 'invoice-new',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const InvoiceFormPage()),
        ),
        GoRoute(
          path: '/invoices/drafts',
          name: 'invoice-drafts',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const DraftsPage()),
        ),
        GoRoute(
          path: '/invoices/:id',
          name: 'invoice-detail',
          parentNavigatorKey: _rootNavigatorKey,
          redirect: (context, state) => _parseId(state) == null ? '/invoices' : null,
          pageBuilder: (context, state) {
            return _buildCupertinoPage(
              state,
              InvoiceDetailPage(
                invoiceId: _parseId(state)!,
                knownStatus:
                    state.extra is String ? state.extra as String : null,
              ),
            );
          },
        ),
        GoRoute(
          path: '/credit-notes',
          name: 'credit-notes',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const CreditNotesPage()),
        ),
        GoRoute(
          path: '/credit-notes/new',
          name: 'credit-note-new',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const CreditNoteFormPage()),
        ),
        GoRoute(
          path: '/credit-notes/:id',
          name: 'credit-note-detail',
          parentNavigatorKey: _rootNavigatorKey,
          redirect: (context, state) => _parseId(state) == null ? '/credit-notes' : null,
          pageBuilder: (context, state) {
            return _buildCupertinoPage(
              state,
              CreditNoteDetailPage(creditNoteId: _parseId(state)!),
            );
          },
        ),
        GoRoute(
          path: '/proformas',
          name: 'proformas',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const ProformasPage()),
        ),
        GoRoute(
          path: '/proformas/new',
          name: 'proforma-new',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const ProformaFormPage()),
        ),
        GoRoute(
          path: '/proformas/:id',
          name: 'proforma-detail',
          parentNavigatorKey: _rootNavigatorKey,
          redirect: (context, state) => _parseId(state) == null ? '/proformas' : null,
          pageBuilder: (context, state) {
            return _buildCupertinoPage(
              state,
              ProformaDetailPage(proformaId: _parseId(state)!),
            );
          },
        ),
        GoRoute(
          path: '/referral-guides',
          name: 'referral-guides',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const ReferralGuidesPage()),
        ),
        GoRoute(
          path: '/referral-guides/new',
          name: 'referral-guide-new',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const ReferralGuideFormPage()),
        ),
        GoRoute(
          path: '/referral-guides/:id',
          name: 'referral-guide-detail',
          parentNavigatorKey: _rootNavigatorKey,
          redirect: (context, state) => _parseId(state) == null ? '/referral-guides' : null,
          pageBuilder: (context, state) {
            return _buildCupertinoPage(
              state,
              ReferralGuideDetailPage(guideId: _parseId(state)!),
            );
          },
        ),
        GoRoute(
          path: '/customers/new',
          name: 'customer-new',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const CustomerFormPage()),
        ),
        GoRoute(
          path: '/customers/:id',
          name: 'customer-detail',
          parentNavigatorKey: _rootNavigatorKey,
          redirect: (context, state) => _parseId(state) == null ? '/customers' : null,
          pageBuilder: (context, state) {
            return _buildCupertinoPage(
              state,
              CustomerDetailPage(customerId: _parseId(state)!),
            );
          },
        ),
        GoRoute(
          path: '/customers/:id/edit',
          name: 'customer-edit',
          parentNavigatorKey: _rootNavigatorKey,
          redirect: (context, state) => _parseId(state) == null ? '/customers' : null,
          pageBuilder: (context, state) {
            return _buildCupertinoPage(
              state,
              CustomerFormPage(customerId: _parseId(state)!),
            );
          },
        ),
        GoRoute(
          path: '/products/new',
          name: 'product-new',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const ProductFormPage()),
        ),
        GoRoute(
          path: '/products/:id',
          name: 'product-detail',
          parentNavigatorKey: _rootNavigatorKey,
          redirect: (context, state) => _parseId(state) == null ? '/products' : null,
          pageBuilder: (context, state) {
            return _buildCupertinoPage(
              state,
              ProductDetailPage(productId: _parseId(state)!),
            );
          },
        ),
        GoRoute(
          path: '/products/:id/edit',
          name: 'product-edit',
          parentNavigatorKey: _rootNavigatorKey,
          redirect: (context, state) => _parseId(state) == null ? '/products' : null,
          pageBuilder: (context, state) {
            return _buildCupertinoPage(
              state,
              ProductFormPage(productId: _parseId(state)!),
            );
          },
        ),
        GoRoute(
          path: '/reports',
          name: 'reports',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const ReportsPage()),
        ),
        GoRoute(
          path: '/reports/by-period',
          name: 'reports-by-period',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const SalesByPeriodPage()),
        ),
        GoRoute(
          path: '/reports/by-client',
          name: 'reports-by-client',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const SalesByClientPage()),
        ),
        GoRoute(
          path: '/reports/by-product',
          name: 'reports-by-product',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const SalesByProductPage()),
        ),
        GoRoute(
          path: '/reports/pending',
          name: 'reports-pending',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const PendingInvoicesPage()),
        ),
        GoRoute(
          path: '/profile',
          name: 'profile',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const ProfilePage()),
        ),
        GoRoute(
          path: '/profile/edit',
          name: 'profile-edit',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const EditProfilePage()),
        ),
        GoRoute(
          path: '/profile/change-password',
          name: 'profile-change-password',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const ChangePasswordPage()),
        ),
        GoRoute(
          path: '/settings',
          name: 'settings',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const SettingsPage()),
        ),
        GoRoute(
          path: '/select-company',
          name: 'select-company',
          parentNavigatorKey: _rootNavigatorKey,
          pageBuilder: (context, state) =>
              _buildCupertinoPage(state, const CompanySelectionPage()),
        ),
      ],
    );
  }
}
