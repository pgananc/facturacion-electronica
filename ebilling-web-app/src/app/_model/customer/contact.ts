export class Contact {
  idContact: number = 0;
  contactType: string = '';
  value: string = '';
  status: boolean = true;

  constructor(
    idContact?: number,
    contactType?: string,
    value?: string,
    status?: boolean
  ) {
    if (idContact && contactType && value) {
      this.idContact = idContact;
      this.contactType = contactType;
      this.value = value;
      this.status = true;
    }
    if (contactType && value && status) {
      this.contactType = contactType;
      this.value = value;
      this.status = status;
    }
  }
}
