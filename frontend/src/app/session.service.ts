import { Injectable } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';
import { Movie } from './models/movie.model';

@Injectable({
    providedIn: 'root'
})
export class SessionService {
    private sessionId: string;

    constructor() {
        this.sessionId = localStorage.getItem('sessionId') || uuidv4();
        this.setSessionId(this.sessionId);
    }

    setSessionId(sessionId: string) {
        localStorage.setItem('sessionId', sessionId);
    }

    getSessionId(): string {
        return this.sessionId;
    }

    setMovies(movies: Movie[]) {
        localStorage.setItem('movies', JSON.stringify(movies));
    }

    getMovies(): Movie[] {
        return JSON.parse(localStorage.getItem('movies') || '[]');
    }

    setIndex(index: number) {
        localStorage.setItem('index', index.toString());
    }

    getIndex(): number {
        return parseInt(localStorage.getItem('index') || '-1');
    }

    setSeed(seed: string) {
        localStorage.setItem('seed', seed);
    }

    getSeed(): string {
        return localStorage.getItem('seed') || '';
    }
}
