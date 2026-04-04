import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { SelectProductDialogComponent } from './select-product-dialog.component';
import { LoggerService } from '../../../_service/logger/logger.service';

describe('SelectProductDialogComponent', () => {
  let component: SelectProductDialogComponent;
  let fixture: ComponentFixture<SelectProductDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SelectProductDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: {} },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { products: [], searchTerm: '' },
        },
        {
          provide: LoggerService,
          useValue: jasmine.createSpyObj('LoggerService', [
            'debug',
            'info',
            'warn',
            'error',
          ]),
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SelectProductDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
