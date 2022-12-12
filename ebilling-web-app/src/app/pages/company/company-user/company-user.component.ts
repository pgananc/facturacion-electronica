import {Component, OnInit, ViewChild} from '@angular/core';
import {MatPaginator} from '@angular/material/paginator';
import {MatSort} from '@angular/material/sort';
import {MatTableDataSource} from '@angular/material/table';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ActivatedRoute} from '@angular/router';
import {DURATION_TIME_MESSAGE, EMPTY_DATA, HEADER_MESSAGE} from '../../../_constants/constants';
import {FormGroup} from '@angular/forms';
import {Company} from "../../../_model/customer/company";
import {CompanyService} from "../../../_service/customer/company.service";
import {User} from "../../../_model/auth/user";
import {CompanyUserService} from "../../../_service/auth/companyUser.service";
import {UserService} from "../../../_service/user/user.service";

@Component({
  selector: 'app-company',
  templateUrl: 'company-user.component.html',
  styleUrls: ['company-user.component.css'],
})
export class CompanyUserComponent implements OnInit {
  form: FormGroup;
  companies: Company[] = [];
  companyId: number;
  userId: number;
  displayedColumnsClient = ['name', 'username', 'mail', 'actions'];
  dsCurrentUsersInCompany: MatTableDataSource<User>;
  dsAvailableUsers: MatTableDataSource<User>;
  status = true;
  quantityCurrentUsers = 0;
  quantityAvailableUsers = 0;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  constructor(
    private companyService: CompanyService,
    private companyUserService: CompanyUserService,
    private userService: UserService,
    private snackBar: MatSnackBar,
    public route: ActivatedRoute
  ) {
  }

  ngOnInit() {
    this.form = new FormGroup({});
    if (this.companies.length === 0) {
      this.companyService.findAll().subscribe(data => {
        this.companies = data.companyDtos;
      });
    }

    this.companyUserService.messageChange.subscribe((data) => {
      this.snackBar.open(data, HEADER_MESSAGE.MESSAGE_HEADER_INFO.message, {
        duration: 2000,
      });
    });
  }


  delete(companyId: number, userId: number) {
    this.companyUserService.delete(companyId, userId).subscribe((data) => {
      const userDel=this.dsCurrentUsersInCompany.data.find(user=>user.idUser===userId);
      this.dsAvailableUsers.data.push(userDel!);
      this.dsAvailableUsers.data=this.dsAvailableUsers.data;
      this.dsCurrentUsersInCompany.data = this.dsCurrentUsersInCompany.data.filter(user => user.idUser !== userId);
    });
  }

  applyFilter(event: Event) {
    const input = (event.target as HTMLInputElement).value;
    this.dsAvailableUsers!.filter = input.trim().toLowerCase();
  }

  search() {
    this.dsCurrentUsersInCompany = new MatTableDataSource();
    this.companyUserService.searchPageable(0, 10, this.companyId).subscribe((data) => {
      if (data != null) {
        this.quantityCurrentUsers = data.totalElements;
        this.dsCurrentUsersInCompany = new MatTableDataSource(data.content);
        this.dsCurrentUsersInCompany.sort = this.sort!;

        //fill availableUsers
        this.userService.findActiveUsersAndNotInCompany(this.companyId).subscribe((userResponse) => {
          console.log(JSON.stringify(userResponse));
          this.quantityAvailableUsers = userResponse.userDtos.length;
          this.dsAvailableUsers = new MatTableDataSource(userResponse.userDtos);
          this.dsAvailableUsers.sort = this.sort!;
        })

      } else {
        this.snackBar.open(EMPTY_DATA.MESSAGE_EMPTY_DATA.message, 'OK', {
          duration: DURATION_TIME_MESSAGE.value,
        });
      }
    });
  }

  save(idUser: number) {
    this.companyUserService.save({idCompany: this.companyId, idUser: idUser}).subscribe(data => {
      if (data.code === 0) {
        this.companyUserService.findUsersByIdCompany(this.companyId).subscribe(userCompanyResponse => {
          this.dsCurrentUsersInCompany = new MatTableDataSource(userCompanyResponse.userCompanyDto.userDtos);
          this.dsCurrentUsersInCompany.sort = this.sort!;
          this.dsAvailableUsers.data = this.dsAvailableUsers.data.filter(user => user.idUser !== idUser);
        })
      }
    });
  }

  showMore(e: any) {
    this.companyUserService
      .searchPageable(e.pageIndex, e.pageSize, this.companyId)
      .subscribe((data) => {
        this.quantityCurrentUsers = data.totalElements;
        this.dsAvailableUsers = new MatTableDataSource(data.content);
        this.dsAvailableUsers.sort = this.sort!;
      });
  }
}
