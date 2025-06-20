import { Injectable } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';

@Injectable({
    providedIn: 'root'
})
export class SessionService {
    private sessionId: string;

    constructor() {
        this.sessionId = this.generateSessionId();
    }

    private generateSessionId(): string {
        const existingId = sessionStorage.getItem('session_id');
        if (existingId) {
            return existingId;
        }

        const newId = uuidv4();
        sessionStorage.setItem('session_id', newId);
        return newId;
    }

    getSessionId(): string {
        return this.sessionId;
    }
}
