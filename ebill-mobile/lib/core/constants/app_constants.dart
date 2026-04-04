class AppConstants {
  // Pagination
  static const int defaultPageSize = 20;
  static const int maxPageSize = 100;

  // Date Formats
  static const String dateFormat = 'yyyy-MM-dd';
  static const String dateTimeFormat = 'yyyy-MM-dd HH:mm:ss';
  static const String displayDateFormat = 'dd/MM/yyyy';
  static const String displayDateTimeFormat = 'dd/MM/yyyy HH:mm';

  // Currency
  static const String defaultCurrency = 'USD';
  static const String currencySymbol = '\$';

  // Cache TTL
  static const Duration cacheTTL = Duration(minutes: 5);
  static const Duration longCacheTTL = Duration(hours: 1);
}

