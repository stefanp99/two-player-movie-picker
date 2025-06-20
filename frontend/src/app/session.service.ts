import { Injectable } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';
import { Movie } from './models/movie.model';

@Injectable({
    providedIn: 'root'
})
export class SessionService {
    constructor() {
        this.setSessionId();
    }

    setSessionId() {
        const sessionId = localStorage.getItem('sessionId') || uuidv4();
        localStorage.setItem('sessionId', sessionId);
    }

    getSessionId(): string {
        return localStorage.getItem('sessionId') || '';
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
        return parseInt(localStorage.getItem('index') || '0');
    }

    setSeed(seed: string) {
        localStorage.setItem('seed', seed);
    }

    getSeed(): string {
        return localStorage.getItem('seed') || '';
    }

    setLiked(movieIds: number[]) {
        localStorage.setItem('liked', JSON.stringify(movieIds));
    }

    getLiked(): number[] {
        return JSON.parse(localStorage.getItem('liked') || '[]');
    }

    clearAll() {
        localStorage.clear();
    }
}
