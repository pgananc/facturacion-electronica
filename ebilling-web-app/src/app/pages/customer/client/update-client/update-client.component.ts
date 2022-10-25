import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Client } from '../../../../_model/customer/client';
import { ClientService } from '../../../../_service/customer/client.service';
import { ActivatedRoute, Params, Route, Router } from '@angular/router';
import { switchMap } from 'rxjs';
import { Contact } from '../../../../_model/customer/contact';
import { CONTACT_TYPE_ADRESS } from '../../../../_constants/constants';
import {
  CONTACT_TYPE_MOBIL,
  CONTACT_TYPE_MAIL,
} from '../../../../_constants/constants';
import { CONTACT_TYPE_PHONE } from '../../../../_constants/constants';
import {
  CLIENT_TYPE,
  UPDATE,
  SUCCESS,
  IDENTIFICATION_TYPE,
} from '../../../../_constants/constants';

@Component({
  selector: 'app-update-client',
  templateUrl: './update-client.component.html',
  styleUrls: ['./update-client.component.css'],
})
export class UpdateClientComponent implements OnInit {
  idClient: number = 0;
  client: Client = new Client();
  phone: Contact = new Contact();
  mobil: Contact = new Contact();
  mail: Contact = new Contact();
  address: Contact = new Contact();

  form: FormGroup;
  edition: boolean = false;
  clientTypes = CLIENT_TYPE;
  identificationTypes = IDENTIFICATION_TYPE;
  idType: number = 0;
  clientType: number = 0;

  constructor(
    private clientService: ClientService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.client = new Client();
    this.form = new FormGroup({
      id: new FormControl(0),
      identification: new FormControl('', Validators.required),
      name: new FormControl(''),
      phone: new FormControl(''),
      mobil: new FormControl(''),
      email: new FormControl('', [Validators.email]),
      address: new FormControl(''),
      status: new FormControl(true),
    });
    this.route.params.subscribe((params: Params) => {
      this.idClient = params['idClient'];
      this.edition = params['idClient'] != null;
      this.initForm();
    });
  }
  initForm() {
    console.log(this.form.controls['email'].status);
    if (this.edition) {
      this.clientService.findById(this.idClient).subscribe((data) => {
        if (data.code == 0 && data.clientDtos.length > 0) {
          let client = data.clientDtos[0];
          this.phone = this.findContact(
            client.contacts,
            CONTACT_TYPE_PHONE.code
          );
          this.mobil = this.findContact(
            client.contacts,
            CONTACT_TYPE_MOBIL.code
          );
          this.address = this.findContact(
            client.contacts,
            CONTACT_TYPE_ADRESS.code
          );
          this.mail = this.findContact(client.contacts, CONTACT_TYPE_MAIL.code);
          this.idType = client.idType;
          this.clientType = client.clientType;

          this.form = new FormGroup({
            id: new FormControl(client.idClient),
            identification: new FormControl(client.identification),
            name: new FormControl(client.name),
            phone: new FormControl(this.phone.value),
            mobil: new FormControl(this.mobil.value),
            email: new FormControl(this.mail.value),
            address: new FormControl(this.address.value),
            status: new FormControl(client.status),
          });
        }
      });
    }
  }

  findContact(contacts: Contact[], contactType: string): Contact {
    let contact = contacts.find((c) => c.contactType === contactType);
    if (contact === undefined) {
      return new Contact();
    } else {
      return contact;
    }
  }
  save() {
    this.client.idClient = this.idClient;
    this.client.idType = this.idType;
    this.client.clientType = this.clientType;
    this.client.identification = this.form.value['identification'];
    this.client.name = this.form.value['name'];
    this.client.status = this.form.value['status'];
    this.client.contacts = this.addContacts(this.form);
    //let userName = sessionStorage.getItem(environment.USER);
    if (this.client != null && this.client.idClient > 0) {
      // this.employee.modifiedBy = userName != null ? userName : '';
      this.clientService
        .update(this.client, this.client.idClient)
        .pipe(
          switchMap(() => {
            return this.clientService.findAll();
          })
        )
        .subscribe((data) => {
          this.clientService.clientChange.next(data.clientDtos);
          this.clientService.messangeChange.next(
            UPDATE.MESSAGE_UPDATE_CLIENT.message
          );
        });
    } else {
      // this.employee.createdBy = userName != null ? userName : '';
      this.clientService
        .save(this.client)
        .pipe(
          switchMap(() => {
            return this.clientService.findAll();
          })
        )
        .subscribe((data) => {
          this.clientService.clientChange.next(data.clientDtos);
          this.clientService.messangeChange.next(
            SUCCESS.MESSAGE_REGISTER_CLIENT.message
          );
        });
    }

    this.router.navigate(['client']);
  }

  addContacts(form: FormGroup): Contact[] {
    let contacts = [];
    let phone = this.form.value['phone'];
    let mobil = this.form.value['mobil'];
    let address = this.form.value['address'];
    let email = this.form.value['email'];
    if (phone != '') {
      contacts.push(
        new Contact(this.phone.idContact, CONTACT_TYPE_PHONE.code, phone, true)
      );
    }
    if (address != '') {
      contacts.push(
        new Contact(
          this.address.idContact,
          CONTACT_TYPE_ADRESS.code,
          address,
          true
        )
      );
    }
    if (mobil != '') {
      contacts.push(
        new Contact(this.mobil.idContact, CONTACT_TYPE_MOBIL.code, mobil, true)
      );
    }
    if (email != '') {
      contacts.push(
        new Contact(this.mail.idContact, CONTACT_TYPE_MAIL.code, email, true)
      );
    }

    return contacts;
  }

  get f() {
    return this.form.controls;
  }

  validateSave() {
    return this.idType;
  }
}
