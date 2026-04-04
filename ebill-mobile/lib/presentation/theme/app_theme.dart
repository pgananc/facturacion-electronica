import 'package:flutter/material.dart';

class AppTheme {
  static const Color _seed = Color(0xFF001255);

  /// Tokens alineados con `ebilling-web-app/src/theme.scss` → `[data-theme="dark"]`
  /// (cyan/sky + fondos slate; no el mismo resultado que `fromSeed(#001255)`).
  static ColorScheme get _darkWebColorScheme {
    const primary = Color(0xFF38BDF8);
    const background = Color(0xFF0D1B2A);
    const surface = Color(0xFF162A42);
    const surfaceAlt = Color(0xFF1E3350);
    const inputBg = Color(0xFF132338);
    const textPrimary = Color(0xFFE8EDF5);
    const textSecondary = Color(0xFF94A3B8);
    final base = ColorScheme.fromSeed(
      seedColor: primary,
      brightness: Brightness.dark,
    );
    return base.copyWith(
      primary: primary,
      onPrimary: Colors.white,
      primaryContainer: surfaceAlt,
      onPrimaryContainer: textPrimary,
      secondary: const Color(0xFF8EF6D3),
      onSecondary: background,
      secondaryContainer: inputBg,
      onSecondaryContainer: textPrimary,
      tertiary: const Color(0xFF7DD3FC),
      onTertiary: background,
      tertiaryContainer: surfaceAlt,
      onTertiaryContainer: textPrimary,
      surface: surface,
      onSurface: textPrimary,
      onSurfaceVariant: textSecondary,
      surfaceContainerHighest: surfaceAlt,
      surfaceContainerHigh: surfaceAlt,
      surfaceContainer: surface,
      surfaceContainerLow: const Color(0xFF111F33),
      surfaceContainerLowest: background,
      outline: const Color(0xFF64748B),
      outlineVariant: const Color(0xFF334155),
      error: const Color(0xFFF87171),
      onError: background,
      inverseSurface: textPrimary,
      onInverseSurface: surface,
      inversePrimary: const Color(0xFF0284C7),
      surfaceTint: Colors.transparent,
    );
  }

  static ThemeData get lightTheme {
    final colorScheme = ColorScheme.fromSeed(
      seedColor: _seed,
      brightness: Brightness.light,
    );
    return _build(colorScheme);
  }

  static ThemeData get darkTheme => _build(
        _darkWebColorScheme,
        scaffoldBackground: const Color(0xFF0D1B2A),
      );

  static ThemeData _build(
    ColorScheme cs, {
    Color? scaffoldBackground,
  }) {
    final base = ThemeData(useMaterial3: true, colorScheme: cs);
    final textTheme = base.textTheme.apply(
      bodyColor: cs.onSurface,
      displayColor: cs.onSurface,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: cs,
      textTheme: textTheme,
      scaffoldBackgroundColor: scaffoldBackground ??
          (cs.brightness == Brightness.light
              ? const Color(0xFFF6F7FB)
              : cs.surface),
      appBarTheme: AppBarTheme(
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 1,
        backgroundColor: cs.surface,
        surfaceTintColor: Colors.transparent,
        titleTextStyle: textTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.w700,
          color: cs.onSurface,
        ),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        color: cs.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.3)),
        ),
        margin: EdgeInsets.zero,
      ),
      inputDecorationTheme: InputDecorationTheme(
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: cs.outlineVariant),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.5)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: cs.primary, width: 2),
        ),
        filled: true,
        fillColor: cs.surface,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          textStyle: textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w600),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          elevation: 0,
          textStyle: textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w600),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          side: BorderSide(color: cs.outline),
        ),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: cs.primary,
        foregroundColor: cs.onPrimary,
        elevation: 2,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
      ),
      chipTheme: ChipThemeData(
        shape: const StadiumBorder(),
        labelStyle: textTheme.labelSmall?.copyWith(fontWeight: FontWeight.w600),
      ),
      navigationBarTheme: NavigationBarThemeData(
        indicatorColor: cs.primaryContainer,
        labelTextStyle: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return TextStyle(
              color: cs.primary,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            );
          }
          return TextStyle(color: cs.onSurfaceVariant, fontSize: 12);
        }),
      ),
      dividerTheme: DividerThemeData(
        color: cs.outlineVariant.withValues(alpha: 0.3),
        thickness: 1,
        space: 1,
      ),
      listTileTheme: ListTileThemeData(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }
}
