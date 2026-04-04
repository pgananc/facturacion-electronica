import '../../domain/entities/invoice_item.dart';
import '../../domain/entities/product.dart';

/// Totales y detalles para enviar al API de facturación (IVA / ICE).
class InvoiceTotals {
  InvoiceTotals({
    required this.subtotalZero,
    required this.subtotalIva,
    required this.subtotalIce,
    required this.totalAmount,
    required this.invoiceDetails,
  });

  final double subtotalZero;
  final double subtotalIva;
  final double subtotalIce;
  final double totalAmount;
  final List<Map<String, dynamic>> invoiceDetails;
}

/// Calcula bases e impuestos a partir de líneas de factura.
///
/// [ivaPercent] e [icePercent] vienen del producto (tax type). Si no hay tipo,
/// se asume IVA 15% para bienes gravados (Ecuador) salvo [idTaxType] == 0.
class InvoiceTaxCalculator {
  static const double defaultIvaPercent = 15;

  static InvoiceTotals compute(List<InvoiceItem> lines) {
    double subtotalZero = 0;
    double subtotalIvaBase = 0;
    double subtotalIceSum = 0;
    double totalAmount = 0;
    final details = <Map<String, dynamic>>[];

    for (final line in lines) {
      final p = line.product;
      final qty = line.quantity;
      final base = _round2(p.unitPrice * qty);

      final ivaRate = _resolveIvaPercent(p);
      final iceRate = _resolveIcePercent(p);

      final ivaValue = _round2(base * (ivaRate / 100));
      final iceValue = _round2(base * (iceRate / 100));

      if (ivaRate <= 0) {
        subtotalZero += base;
      } else {
        subtotalIvaBase += base;
      }
      subtotalIceSum += iceValue;
      final lineTotal = _round2(base + ivaValue + iceValue);
      totalAmount += lineTotal;

      details.add({
        'quantity': qty,
        'unitPrice': p.unitPrice,
        'discount': 0,
        'subtotal': base,
        'ivaPercentage': ivaRate,
        'ivaValue': ivaValue,
        'iceValue': iceValue,
        'productDto': {
          'idProduct': p.id,
        },
      });
    }

    return InvoiceTotals(
      subtotalZero: _round2(subtotalZero),
      subtotalIva: _round2(subtotalIvaBase),
      subtotalIce: _round2(subtotalIceSum),
      totalAmount: _round2(totalAmount),
      invoiceDetails: details,
    );
  }

  static double _resolveIvaPercent(Product p) {
    if (p.ivaPercent != null) {
      return p.ivaPercent!;
    }
    if (p.idTaxType != null && p.idTaxType == 0) {
      return 0;
    }
    if (p.hasTourismIva) {
      return 8;
    }
    return defaultIvaPercent;
  }

  static double _resolveIcePercent(Product p) {
    if (!p.hasIce) return 0;
    return p.icePercent ?? 0;
  }

  static double _round2(double v) => (v * 100).round() / 100;

  static List<Map<String, dynamic>> buildProformaDetailPayloads(
    List<InvoiceItem> lines,
  ) {
    final details = <Map<String, dynamic>>[];
    for (var i = 0; i < lines.length; i++) {
      final line = lines[i];
      final p = line.product;
      final qty = line.quantity;
      final ivaRate = _resolveIvaPercent(p);
      final iceRate = _resolveIcePercent(p);
      details.add({
        'idProduct': p.id,
        'productCode': p.mainCode,
        'description': p.name,
        'quantity': qty,
        'unitPrice': p.unitPrice,
        'discount': 0,
        'ivaPercentage': ivaRate,
        'icePercentage': iceRate,
        'lineOrder': i + 1,
      });
    }
    return details;
  }
}
