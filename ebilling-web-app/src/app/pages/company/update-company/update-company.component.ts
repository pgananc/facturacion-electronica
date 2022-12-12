import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { switchMap } from 'rxjs';
import {
  DURATION_TIME_MESSAGE,
  EXIST_DATA,
  HEADER_MESSAGE,
  SUCCESS,
  UPDATE,
} from '../../../_constants/constants';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Company } from '../../../_model/customer/company';
import { CompanyService } from '../../../_service/customer/company.service';

@Component({
  selector: 'app-update-company',
  templateUrl: './update-company.component.html',
  styleUrls: ['./update-company.component.css'],
})
export class UpdateCompanyComponent implements OnInit {
  idCompany: number = 0;
  company: Company = new Company();
  form: FormGroup;
  edition: boolean = false;
  branchOfficeCode: string = '';
  identification: string = '';
  name: string = '';
  forceToAccounting = true;
  principal = true;

  constructor(
    private companyService: CompanyService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.company = new Company();
    this.form = new FormGroup({
      id: new FormControl(0),
      branchOfficeCode: new FormControl(''),
      identification: new FormControl('', Validators.required),
      name: new FormControl(''),
      status: new FormControl(true),
      forceToAccounting: new FormControl(true),
      principal: new FormControl(true),
    });
    this.route.params.subscribe((params: Params) => {
      this.idCompany = params['idCompany'];
      this.edition = params['idCompany'] != null;
      this.initForm();
    });
  }

  initForm() {
    if (this.edition) {
      this.companyService.findById(this.idCompany).subscribe((data) => {
        if (data.code == 0 && data.companyDto) {
          let company = data.companyDto;
          this.identification = company.identification;
          this.branchOfficeCode = company.branchOfficeCode;
          this.name = company.name;
          this.forceToAccounting = company.forcedToAccounting === 'S';
          this.principal = company.principal === 'S';

          this.form = new FormGroup({
            id: new FormControl(company.idCompany),
            identification: new FormControl(company.identification),
            branchOfficeCode: new FormControl(company.branchOfficeCode),
            name: new FormControl(company.name),
            status: new FormControl(company.status),
            forceToAccounting: new FormControl(
              company.forcedToAccounting === 'S'
            ),
            principal: new FormControl(company.principal === 'S'),
          });
        }
      });
    }
  }

  validateSave() {
    if (
      this.edition &&
      this.identification === this.form.value['identification']
    ) {
      this.save();
    } else {
      this.validateIdentification();
    }
  }

  validateIdentification() {
    this.companyService
      .existsByIdentificationAndBranchOfficeCode(
        this.form.value['identification'],
        this.form.value['branchOfficeCode']
      )
      .subscribe((data) => {
        if (data) {
          this.snackBar.open(
            EXIST_DATA.MESSAGE_EXISTS_IDENTIFICATION.message,
            HEADER_MESSAGE.MESSAGE_HEADER_INFO.message,
            {
              duration: DURATION_TIME_MESSAGE.value,
            }
          );
        } else {
          this.save();
        }
      });
  }

  save() {
    this.company.idCompany = this.idCompany;
    this.company.identification = this.form.value['identification'];
    this.company.branchOfficeCode = this.form.value['branchOfficeCode'];
    this.company.name = this.form.value['name'];
    this.company.status = this.form.value['status'];
    this.company.forcedToAccounting = this.form.value['forceToAccounting']
      ? 'S'
      : 'N';
    console.log(`forceToAccounting: ${this.company.forcedToAccounting}`);
    this.company.principal = this.form.value['principal'] ? 'S' : 'N';

    if (this.company.idCompany > 0) {
      this.companyService
        .update(this.company, this.company.idCompany)
        .pipe(
          switchMap(() => {
            return this.companyService.findAll();
          })
        )
        .subscribe((data) => {
          this.companyService.companiesChange.next(data.companyDtos);
          this.companyService.messageChange.next(
            UPDATE.MESSAGE_UPDATE_COMPANY.message
          );
        });
    } else {
      this.companyService
        .save(this.company)
        .pipe(
          switchMap(() => {
            return this.companyService.findAll();
          })
        )
        .subscribe((data) => {
          this.companyService.companiesChange.next(data.companyDtos);
          this.companyService.messageChange.next(
            SUCCESS.MESSAGE_REGISTER_COMPANY.message
          );
        });
    }

    this.router.navigate(['company']);
  }

  get f() {
    return this.form.controls;
  }

  clearMainCode() {
    this.form.get('mainCode')?.reset();
  }
}
