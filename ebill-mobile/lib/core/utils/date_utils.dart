import 'package:intl/intl.dart';

import '../constants/app_constants.dart';

class DateUtils {
  static String formatDate(DateTime date, {String? format}) {
    return DateFormat(format ?? AppConstants.displayDateFormat).format(date);
  }

  static String formatDateTime(DateTime date, {String? format}) {
    return DateFormat(format ?? AppConstants.displayDateTimeFormat).format(date);
  }

  static DateTime? parseDate(String dateString, {String? format}) {
    try {
      return DateFormat(format ?? AppConstants.dateFormat).parse(dateString);
    } catch (e) {
      return null;
    }
  }

  static String formatCurrency(double amount, {String? symbol}) {
    final formatter = NumberFormat.currency(
      locale: 'es',
      symbol: symbol ?? AppConstants.currencySymbol,
      decimalDigits: 2,
    );
    return formatter.format(amount);
  }
}

