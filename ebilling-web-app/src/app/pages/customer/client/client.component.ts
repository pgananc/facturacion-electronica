import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Client } from '../../../_model/customer/client';
import { ClientService } from '../../../_service/customer/client.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import {
  HEADER_MESSAGE,
  DELETE,
  CLIENT_TYPE,
} from '../../../_constants/constants';
import { FormGroup, FormControl } from '@angular/forms';
import { EMPTY_DATA } from '../../../_constants/constants';

@Component({
  selector: 'app-client',
  templateUrl: './client.component.html',
  styleUrls: ['./client.component.css'],
})
export class ClientComponent implements OnInit {
  form: FormGroup;
  clientTypes = CLIENT_TYPE;
  displayedColumns = ['identification', 'name', 'status', 'actions'];
  dataSource: MatTableDataSource<Client>;
  clientType: number = 0;
  status: boolean = true;
  quantity: number = 0;

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private clientService: ClientService,
    private snackBar: MatSnackBar,
    public route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.form = new FormGroup({
      identification: new FormControl(''),
      name: new FormControl(''),
    });
    this.status = true;
    this.clientService.clientChange.subscribe((data) => {
      this.dataSource = new MatTableDataSource(data);
      this.dataSource.sort = this.sort;
      this.dataSource.paginator = this.paginator;
    });

    this.clientService.messangeChange.subscribe((data) => {
      this.snackBar.open(data, HEADER_MESSAGE.MESSAGE_HEADER_INFO.message, {
        duration: 2000,
      });
    });
    let client = new Client();
    client.identification = this.form.value['identification'];
    client.name = this.form.value['name'];
    client.clientType = this.clientType;
    client.status = this.status;
    this.clientService.searchPageable(0, 5, client).subscribe((data) => {
      console.log(data);
      this.quantity = data.totalElements;
      this.dataSource = new MatTableDataSource(data.content);
      this.dataSource.sort = this.sort;
      //this.dataSource.paginator = this.paginator;
    });
  }

  delete(idClient: number) {
    this.clientService.delete(idClient).subscribe(() => {
      this.clientService.findAll().subscribe((data) => {
        if (data.code == 0) {
          this.clientService.clientChange.next(data.clientDtos);
          this.clientService.messangeChange.next(
            DELETE.MESSAGE_DELETE_CLIENT.message
          );
        } else {
          console.log(data.message);
        }
      });
    });
  }
  applyFilter(event: Event) {
    const input = (event.target as HTMLInputElement).value;
    this.dataSource.filter = input.trim().toLowerCase();
  }

  search() {
    let client = new Client();
    client.identification = this.form.value['identification'];
    client.name = this.form.value['name'];
    client.clientType = this.clientType;
    client.status = this.status;
    this.clientService.searchPageable(0, 5, client).subscribe((data) => {
      if (data != null) {
        this.quantity = data.totalElements;
        this.dataSource = new MatTableDataSource(data.content);
        this.dataSource.sort = this.sort;
        //this.dataSource.paginator = this.paginator;
      } else {
        this.snackBar.open(EMPTY_DATA.MESSAGE_EMPTY_DATA.message, 'OK', {
          duration: 5000,
        });
      }
    });
  }
  limpiar() {
    this.form = new FormGroup({
      identification: new FormControl(''),
      name: new FormControl(''),
    });
    this.clientType = 0;
    this.status = true;
  }

  showMore(e: any) {
    let client = new Client();
    client.identification = this.form.value['identification'];
    client.name = this.form.value['name'];
    client.clientType = this.clientType;
    client.status = this.status;
    this.clientService
      .searchPageable(e.pageIndex, e.pageSize, client)
      .subscribe((data) => {
        this.quantity = data.totalElements;
        this.dataSource = new MatTableDataSource(data.content);
        this.dataSource.sort = this.sort;
        //this.dataSource.paginator = this.paginator;
      });
  }
}