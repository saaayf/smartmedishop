import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FraudAlert } from '../../core/services/api.service';

@Component({
  selector: 'app-resolve-fraud-alert-dialog',
  templateUrl: './resolve-fraud-alert-dialog.component.html',
  styleUrls: ['./resolve-fraud-alert-dialog.component.scss']
})
export class ResolveFraudAlertDialogComponent {
  resolveForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<ResolveFraudAlertDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public alert: FraudAlert,
    private fb: FormBuilder
  ) {
    this.resolveForm = this.fb.group({
      investigationNotes: [this.alert.investigationNotes || '', [Validators.required, Validators.minLength(10)]]
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onResolve(): void {
    if (this.resolveForm.valid) {
      const notes = this.resolveForm.get('investigationNotes')?.value;
      this.dialogRef.close(notes);
    }
  }

  getErrorMessage(): string {
    const control = this.resolveForm.get('investigationNotes');
    if (control?.hasError('required')) {
      return 'Investigation notes are required';
    }
    if (control?.hasError('minlength')) {
      return 'Investigation notes must be at least 10 characters';
    }
    return '';
  }

  getFraudScoreClass(score: number | undefined): string {
    if (!score) return 'none';
    if (score < 0.3) return 'low';
    if (score < 0.6) return 'medium';
    return 'high';
  }
}

