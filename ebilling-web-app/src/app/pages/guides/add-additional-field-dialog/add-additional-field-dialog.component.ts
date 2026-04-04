import { Component, OnInit, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

export interface AdditionalFieldData {
  name: string;
  description: string;
}

@Component({
  selector: 'app-add-additional-field-dialog',
  templateUrl: './add-additional-field-dialog.component.html',
  styleUrls: ['./add-additional-field-dialog.component.css'],
})
export class AddAdditionalFieldDialogComponent implements OnInit {
  fieldForm: FormGroup;
  isEdit = false;

  constructor(
    private readonly dialogRef: MatDialogRef<AddAdditionalFieldDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AdditionalFieldData | null,
    private readonly fb: FormBuilder
  ) {
    this.isEdit = !!data;
    this.fieldForm = this.fb.group({
      name: [data?.name || '', [Validators.required, Validators.maxLength(100)]],
      description: [data?.description || '', [Validators.required, Validators.maxLength(500)]],
    });
  }

  ngOnInit(): void {}

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onSave(): void {
    if (this.fieldForm.invalid) {
      this.fieldForm.markAllAsTouched();
      return;
    }

    const fieldData: AdditionalFieldData = {
      name: this.fieldForm.value.name.trim(),
      description: this.fieldForm.value.description.trim(),
    };

    this.dialogRef.close(fieldData);
  }
}

