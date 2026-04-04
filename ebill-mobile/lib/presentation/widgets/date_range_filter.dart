import 'package:flutter/material.dart';

import '../../core/utils/date_utils.dart' as app_date;

class DateRangeFilter extends StatelessWidget {
  const DateRangeFilter({
    super.key,
    required this.dateFrom,
    required this.dateTo,
    required this.onChanged,
    this.onSearch,
  });

  final DateTime dateFrom;
  final DateTime dateTo;
  final void Function(DateTime from, DateTime to) onChanged;
  final VoidCallback? onSearch;

  Future<void> _pickFrom(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: dateFrom,
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
      helpText: 'Fecha desde',
    );
    if (picked != null) {
      onChanged(picked, dateTo.isBefore(picked) ? picked : dateTo);
    }
  }

  Future<void> _pickTo(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: dateTo,
      firstDate: dateFrom,
      lastDate: DateTime.now(),
      helpText: 'Fecha hasta',
    );
    if (picked != null) {
      onChanged(dateFrom, picked);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Expanded(
              child: _DateChip(
                label: 'Desde',
                value: app_date.DateUtils.formatDate(dateFrom),
                onTap: () => _pickFrom(context),
                theme: theme,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _DateChip(
                label: 'Hasta',
                value: app_date.DateUtils.formatDate(dateTo),
                onTap: () => _pickTo(context),
                theme: theme,
              ),
            ),
            if (onSearch != null) ...[
              const SizedBox(width: 8),
              FilledButton.tonalIcon(
                onPressed: onSearch,
                icon: const Icon(Icons.search, size: 18),
                label: const Text('Buscar'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _DateChip extends StatelessWidget {
  const _DateChip({
    required this.label,
    required this.value,
    required this.onTap,
    required this.theme,
  });

  final String label;
  final String value;
  final VoidCallback onTap;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        decoration: BoxDecoration(
          border: Border.all(color: theme.colorScheme.outline.withValues(alpha: 0.3)),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 2),
            Row(
              children: [
                Icon(
                  Icons.calendar_today,
                  size: 14,
                  color: theme.colorScheme.primary,
                ),
                const SizedBox(width: 4),
                Expanded(
                  child: Text(
                    value,
                    style: theme.textTheme.bodyMedium,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
