import 'dart:io';

import 'package:dartz/dartz.dart' hide State;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pdfx/pdfx.dart';
import 'package:share_plus/share_plus.dart';

import '../../../core/error/failures.dart';

/// Fullscreen PDF viewer with pinch zoom and share actions.
class PdfViewerPage extends StatefulWidget {
  const PdfViewerPage({
    super.key,
    required this.pdfBytes,
    required this.title,
    required this.pdfFileName,
    this.downloadXml,
    this.xmlFileName = 'documento.xml',
  });

  final Uint8List pdfBytes;
  final String title;
  final String pdfFileName;
  final Future<Either<Failure, List<int>>> Function()? downloadXml;
  final String xmlFileName;

  @override
  State<PdfViewerPage> createState() => _PdfViewerPageState();
}

class _PdfViewerPageState extends State<PdfViewerPage> {
  late final PdfControllerPinch _controller;
  bool _sharingXml = false;

  @override
  void initState() {
    super.initState();
    _controller = PdfControllerPinch(
      document: PdfDocument.openData(widget.pdfBytes),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _sharePdf() async {
    await _shareBytes(widget.pdfBytes, widget.pdfFileName, 'application/pdf');
  }

  Future<void> _shareXml() async {
    final download = widget.downloadXml;
    if (download == null) return;
    setState(() => _sharingXml = true);
    try {
      final r = await download();
      if (!mounted) return;
      await r.fold(
        (f) async {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(f.message)),
          );
        },
        (bytes) async {
          if (bytes.isEmpty) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('El archivo XML está vacío.')),
            );
          } else {
            await _shareBytes(
              Uint8List.fromList(bytes),
              widget.xmlFileName,
              'application/xml',
            );
          }
        },
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              kDebugMode
                  ? 'Error al descargar XML: $e'
                  : 'Error al descargar el documento. Intente de nuevo.',
            ),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _sharingXml = false);
    }
  }

  Rect _shareOrigin() {
    final box = context.findRenderObject() as RenderBox?;
    if (box != null && box.hasSize && box.size != Size.zero) {
      return box.localToGlobal(Offset.zero) & box.size;
    }
    final mq = MediaQuery.of(context).size;
    return Rect.fromCenter(
      center: Offset(mq.width / 2, mq.height / 2),
      width: 1,
      height: 1,
    );
  }

  Future<void> _shareBytes(List<int> bytes, String name, String mime) async {
    File? tempFile;
    try {
      final dir = await getTemporaryDirectory();
      tempFile = File('${dir.path}/$name');
      await tempFile.writeAsBytes(bytes, flush: true);
      await Share.shareXFiles(
        [XFile(tempFile.path, mimeType: mime)],
        text: name,
        sharePositionOrigin: _shareOrigin(),
      );
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.grey.shade900,
        foregroundColor: Colors.white,
        title: Text(
          widget.title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        actions: [
          IconButton(
            tooltip: 'Compartir PDF',
            icon: const Icon(Icons.share_outlined),
            onPressed: _sharePdf,
          ),
          if (widget.downloadXml != null)
            IconButton(
              tooltip: 'Compartir XML',
              icon: _sharingXml
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.code_outlined),
              onPressed: _sharingXml ? null : _shareXml,
            ),
          IconButton(
            tooltip: 'Descargar PDF',
            icon: const Icon(Icons.download_outlined),
            onPressed: _sharePdf,
          ),
        ],
      ),
      body: PdfViewPinch(controller: _controller),
    );
  }
}
