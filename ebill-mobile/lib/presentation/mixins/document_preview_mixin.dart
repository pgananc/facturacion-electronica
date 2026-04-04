import 'dart:io';
import 'dart:typed_data';

import 'package:dartz/dartz.dart' hide State;
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../../core/error/failures.dart';
import '../pages/common/pdf_viewer_page.dart';

/// Shared PDF/XML preview & share functionality for detail pages.
mixin DocumentPreviewMixin<T extends StatefulWidget> on State<T> {
  Uint8List? _docPdfBytes;
  bool docLoadingPdf = false;

  void disposePdfResources() {
    _docPdfBytes = null;
  }

  void _showPdfRetrySnackBar(String message, VoidCallback onRetry) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        action: SnackBarAction(
          label: 'Reintentar',
          onPressed: onRetry,
        ),
      ),
    );
  }

  Future<void> loadAndPreviewPdf(
    Future<Either<Failure, List<int>>> Function() download, {
    String title = 'Documento PDF',
    String pdfFileName = 'documento.pdf',
    Future<Either<Failure, List<int>>> Function()? downloadXml,
    String xmlFileName = 'documento.xml',
  }) async {
    if (docLoadingPdf) return;
    if (_docPdfBytes != null) {
      _openPdfViewer(
        _docPdfBytes!,
        title: title,
        pdfFileName: pdfFileName,
        downloadXml: downloadXml,
        xmlFileName: xmlFileName,
      );
      return;
    }
    setState(() => docLoadingPdf = true);
    try {
      final result = await download();
      if (!mounted) return;
      final bytes = result.fold((l) => null, (r) => r);
      if (bytes == null) {
        setState(() => docLoadingPdf = false);
        result.fold(
          (f) => _showPdfRetrySnackBar(
            f.message,
            () => loadAndPreviewPdf(
              download,
              title: title,
              pdfFileName: pdfFileName,
              downloadXml: downloadXml,
              xmlFileName: xmlFileName,
            ),
          ),
          (_) {},
        );
        return;
      }
      if (bytes.isEmpty) {
        setState(() => docLoadingPdf = false);
        _showPdfRetrySnackBar(
          'El documento PDF está vacío.',
          () => loadAndPreviewPdf(
            download,
            title: title,
            pdfFileName: pdfFileName,
            downloadXml: downloadXml,
            xmlFileName: xmlFileName,
          ),
        );
        return;
      }
      final u = Uint8List.fromList(bytes);
      setState(() {
        _docPdfBytes = u;
        docLoadingPdf = false;
      });
      _openPdfViewer(
        u,
        title: title,
        pdfFileName: pdfFileName,
        downloadXml: downloadXml,
        xmlFileName: xmlFileName,
      );
    } catch (_) {
      if (mounted) {
        setState(() => docLoadingPdf = false);
        _showPdfRetrySnackBar(
          'Error al cargar el PDF.',
          () => loadAndPreviewPdf(
            download,
            title: title,
            pdfFileName: pdfFileName,
            downloadXml: downloadXml,
            xmlFileName: xmlFileName,
          ),
        );
      }
    }
  }

  void _openPdfViewer(
    Uint8List bytes, {
    required String title,
    required String pdfFileName,
    Future<Either<Failure, List<int>>> Function()? downloadXml,
    required String xmlFileName,
  }) {
    Navigator.of(context).push<void>(
      CupertinoPageRoute<void>(
        builder: (_) => PdfViewerPage(
          pdfBytes: bytes,
          title: title,
          pdfFileName: pdfFileName,
          downloadXml: downloadXml,
          xmlFileName: xmlFileName,
        ),
      ),
    );
  }

  Future<void> shareDocumentFile(
    List<int> bytes,
    String name,
    String mime,
  ) async {
    File? tempFile;
    try {
      final dir = await getTemporaryDirectory();
      tempFile = File('${dir.path}/$name');
      await tempFile.writeAsBytes(bytes, flush: true);
      await Share.shareXFiles([XFile(tempFile.path, mimeType: mime)], text: name);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Error al compartir el archivo.')),
        );
      }
    } finally {
      try {
        if (tempFile != null && await tempFile.exists()) {
          await tempFile.delete();
        }
      } catch (_) {}
    }
  }

  Widget buildPdfPreviewButton(
    Future<Either<Failure, List<int>>> Function() download, {
    String title = 'Documento PDF',
    String pdfFileName = 'documento.pdf',
    Future<Either<Failure, List<int>>> Function()? downloadXml,
    String xmlFileName = 'documento.xml',
  }) {
    return IconButton(
      tooltip: 'PDF',
      icon: docLoadingPdf
          ? const SizedBox(
              width: 22,
              height: 22,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : const Icon(Icons.picture_as_pdf),
      onPressed: docLoadingPdf
          ? null
          : () => loadAndPreviewPdf(
                download,
                title: title,
                pdfFileName: pdfFileName,
                downloadXml: downloadXml,
                xmlFileName: xmlFileName,
              ),
    );
  }

  Widget buildSharePopupMenu({
    required Future<Either<Failure, List<int>>> Function() downloadPdf,
    required Future<Either<Failure, List<int>>> Function() downloadXml,
    required String pdfFileName,
    required String xmlFileName,
  }) {
    return PopupMenuButton<String>(
      onSelected: (v) async {
        if (v == 'pdf') {
          final r = await downloadPdf();
          if (!mounted) return;
          r.fold(
            (f) => ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(f.message)),
            ),
            (bytes) {
              if (bytes.isEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('El archivo PDF está vacío.')),
                );
              } else {
                shareDocumentFile(bytes, pdfFileName, 'application/pdf');
              }
            },
          );
        } else if (v == 'xml') {
          final r = await downloadXml();
          if (!mounted) return;
          r.fold(
            (f) => ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(f.message)),
            ),
            (bytes) {
              if (bytes.isEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('El archivo XML está vacío.')),
                );
              } else {
                shareDocumentFile(bytes, xmlFileName, 'application/xml');
              }
            },
          );
        }
      },
      itemBuilder: (c) => const [
        PopupMenuItem(value: 'pdf', child: Text('Compartir PDF')),
        PopupMenuItem(value: 'xml', child: Text('Compartir XML')),
      ],
    );
  }
}
