import 'package:flutter/material.dart';

enum DocumentStatus {
  autorizado,
  pendiente,
  rechazado,
  borrador,
  anulado,
  unknown,
}

DocumentStatus parseDocumentStatus(String? raw) {
  if (raw == null) return DocumentStatus.unknown;
  switch (raw.toUpperCase()) {
    case 'AUTORIZADO':
    case 'AUTHORIZED':
      return DocumentStatus.autorizado;
    case 'PENDIENTE':
    case 'PENDING':
    case 'RECIBIDA':
    case 'EN PROCESAMIENTO':
      return DocumentStatus.pendiente;
    case 'RECHAZADO':
    case 'NO AUTORIZADO':
    case 'DEVUELTA':
      return DocumentStatus.rechazado;
    case 'BORRADOR':
    case 'DRAFT':
      return DocumentStatus.borrador;
    case 'ANULADO':
    case 'CANCELLED':
      return DocumentStatus.anulado;
    default:
      return DocumentStatus.unknown;
  }
}

class StatusChip extends StatelessWidget {
  const StatusChip({super.key, required this.status, this.label});

  final String? status;
  final String? label;

  @override
  Widget build(BuildContext context) {
    final parsed = parseDocumentStatus(status);
    final (Color bg, Color fg) = _colors(parsed, context);
    final text = label ?? status?.toUpperCase() ?? '—';

    final textStyle = Theme.of(context).textTheme.labelSmall?.copyWith(
          color: fg,
          fontWeight: FontWeight.w600,
          letterSpacing: 0.3,
        ) ??
        TextStyle(
          fontSize: 11,
          color: fg,
          fontWeight: FontWeight.w600,
          letterSpacing: 0.3,
        );

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(100),
      ),
      child: Text(
        text,
        style: textStyle,
      ),
    );
  }

  (Color, Color) _colors(DocumentStatus s, BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    switch (s) {
      case DocumentStatus.autorizado:
        return isDark
            ? (const Color(0xFF1B5E20), const Color(0xFFA5D6A7))
            : (const Color(0xFFE8F5E9), const Color(0xFF2E7D32));
      case DocumentStatus.pendiente:
        return isDark
            ? (const Color(0xFF5D4037), const Color(0xFFFFD54F))
            : (const Color(0xFFFFF8E1), const Color(0xFFF57F17));
      case DocumentStatus.rechazado:
        return isDark
            ? (const Color(0xFF4E0000), const Color(0xFFEF9A9A))
            : (const Color(0xFFFFEBEE), const Color(0xFFC62828));
      case DocumentStatus.borrador:
        return isDark
            ? (const Color(0xFF424242), const Color(0xFFBDBDBD))
            : (const Color(0xFFF5F5F5), const Color(0xFF616161));
      case DocumentStatus.anulado:
        return isDark
            ? (const Color(0xFF4A0033), const Color(0xFFF48FB1))
            : (const Color(0xFFFCE4EC), const Color(0xFFAD1457));
      case DocumentStatus.unknown:
        return (
          Theme.of(context).colorScheme.surfaceContainerHigh,
          Theme.of(context).colorScheme.onSurfaceVariant,
        );
    }
  }
}
