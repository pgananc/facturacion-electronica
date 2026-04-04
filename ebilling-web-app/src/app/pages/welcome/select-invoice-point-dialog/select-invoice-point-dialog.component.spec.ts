import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { SelectInvoicePointDialogComponent } from './select-invoice-point-dialog.component';

describe('SelectInvoicePointDialogComponent', () => {
  let component: SelectInvoicePointDialogComponent;
  let fixture: ComponentFixture<SelectInvoicePointDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SelectInvoicePointDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: { points: [] } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SelectInvoicePointDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
