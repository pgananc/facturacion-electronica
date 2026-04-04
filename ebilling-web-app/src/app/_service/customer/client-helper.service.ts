import { Injectable } from '@angular/core';
import { Client } from '../../_model/customer/client';
import {
  CONTACT_TYPE_ADRESS,
  CONTACT_TYPE_MAIL,
  CONTACT_TYPE_MOBIL,
  CONTACT_TYPE_PHONE,
} from '../../_constants/constants';

/**
 * ClientHelperService - Utility service for client-related operations
 *
 * Provides common helper methods to avoid code duplication across components
 */
@Injectable({
  providedIn: 'root',
})
export class ClientHelperService {
  /**
   * Maps contact information from contacts array to client properties
   * Mutates the client objects by adding address, mail, phone, and mobil properties
   *
   * @param clientList Array of clients to process
   */
  addContactInformation(clientList: Client[]): void {
    if (!clientList || !Array.isArray(clientList)) {
      return;
    }

    clientList.forEach((client) => {
      if (!client.contacts || !Array.isArray(client.contacts)) {
        return;
      }

      client.contacts.forEach((contact) => {
        switch (contact.contactType) {
          case CONTACT_TYPE_ADRESS.code:
            client.address = contact.value;
            break;
          case CONTACT_TYPE_MAIL.code:
            client.mail = contact.value;
            break;
          case CONTACT_TYPE_MOBIL.code:
            client.mobil = contact.value;
            break;
          case CONTACT_TYPE_PHONE.code:
            client.phone = contact.value;
            break;
        }
      });
    });
  }

  /**
   * Gets company identification from session storage with validation
   *
   * @param identificationKey The session storage key
   * @returns The company identification string
   * @throws Error if identification is not found in session
   */
  getCompanyIdentificationFromSession(identificationKey: string): string {
    const identification = localStorage.getItem(identificationKey);
    if (!identification || identification.trim() === '') {
      throw new Error('Company identification not found in session storage');
    }
    return identification;
  }
}
