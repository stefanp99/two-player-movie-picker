import { Injectable } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';

@Injectable({
    providedIn: 'root'
})
export class SessionService {
    private sessionId: string;

    constructor() {
        this.sessionId = uuidv4();
    }

    getSessionId(): string {
        return this.sessionId;
    }
}
