import { Component, OnInit, ViewChild } from '@angular/core';
import { FormGroup, FormControl } from '@angular/forms';
import { MatTableDataSource } from '@angular/material/table';
import { User } from '../../_model/auth/user';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { UserService } from '../../_service/user/user.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { HEADER_MESSAGE } from '../../_constants/constants';
import {
  EMPTY_DATA,
  DURATION_TIME_MESSAGE,
  DELETE,
} from '../../_constants/constants';

@Component({
  selector: 'app-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.css'],
})
export class UserComponent implements OnInit {
  form: FormGroup;

  displayedColumns = ['name', 'userName', 'status', 'actions'];
  dataSource: MatTableDataSource<User>;
  status: boolean = true;
  quantity: number = 0;

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private userService: UserService,
    private snackBar: MatSnackBar,
    public route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.form = new FormGroup({
      name: new FormControl(''),
      userName: new FormControl(''),
    });
    this.status = true;
    this.status = true;
    this.userService.userChange.subscribe((data) => {
      this.dataSource = new MatTableDataSource(data);
      this.dataSource.sort = this.sort;
      this.dataSource.paginator = this.paginator;
    });
    this.userService.messangeChange.subscribe((data) => {
      this.snackBar.open(data, HEADER_MESSAGE.MESSAGE_HEADER_INFO.message, {
        duration: 2000,
      });
    });
    let user = new User();
    user.userName = this.form.value['name'];
    user.name = this.form.value['userName'];
    user.status = this.status;
    this.userService.searchPageable(0, 10, user).subscribe((data) => {
      this.quantity = data.totalElements;
      this.dataSource = new MatTableDataSource(data.content);
      this.dataSource.sort = this.sort;
      //this.dataSource.paginator = this.paginator;
    });
  }

  search() {
    let user = new User();
    user.name = this.form.value['name'];
    user.userName = this.form.value['userName'];
    user.status = this.status;
    this.userService.searchPageable(0, 10, user).subscribe((data) => {
      if (data != null) {
        this.quantity = data.totalElements;
        this.dataSource = new MatTableDataSource(data.content);
        this.dataSource.sort = this.sort;
      } else {
        this.snackBar.open(EMPTY_DATA.MESSAGE_EMPTY_DATA.message, 'OK', {
          duration: DURATION_TIME_MESSAGE.value,
        });
      }
    });
  }

  delete(idUser: number) {
    this.userService.delete(idUser).subscribe(() => {
      this.userService.findAll().subscribe((data) => {
        if (data.code == 0) {
          this.userService.userChange.next(data.userDtos);
          this.userService.messangeChange.next(
            DELETE.MESSAGE_DELETE_USER.message
          );
        } else {
          console.log(data.message);
        }
      });
    });
  }

  showMore(e: any) {
    let user = new User();
    user.name = this.form.value['name'];
    user.userName = this.form.value['userName'];
    user.status = this.status;
    this.userService
      .searchPageable(e.pageIndex, e.pageSize, user)
      .subscribe((data) => {
        this.quantity = data.totalElements;
        this.dataSource = new MatTableDataSource(data.content);
        this.dataSource.sort = this.sort;
      });
  }
}
