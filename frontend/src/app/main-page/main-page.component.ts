import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { environment } from '../../environments/environment';
import { Movie } from '../models/movie.model';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { SessionService } from '../session.service';

@Component({
  selector: 'app-main-page',
  imports: [
    CommonModule,

    FormsModule,
    ReactiveFormsModule,

    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatButtonModule,
    MatExpansionModule,

    MovieCardComponent
  ],
  templateUrl: './main-page.component.html',
  styleUrl: './main-page.component.css'
})
export class MainPageComponent implements OnInit {
  seed: string = '';
  index: number = 0;
  canPlayerRejoin: Boolean = false;
  initialMovies: Movie[] = [];

  seedForm: FormGroup;

  constructor(private fb: FormBuilder, private http: HttpClient, private sessionService: SessionService) {
    this.seedForm = this.fb.group({
      seed: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{4}$/)]] // 4-character alphanumeric seed
    });
  }

  ngOnInit(): void {
    this.index = this.sessionService.getIndex(); // Restore session index
    this.seed = this.sessionService.getSeed();   // Restore session seed
    this.playerRejoinCheck();                    // Determine if rejoin is allowed
  }

  generateSeed() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = '';
    for (let i = 0; i < 4; i++) {
      const randomIndex = Math.floor(Math.random() * chars.length);
      result += chars[randomIndex];
    }
    this.seed = result;

    this.createRoom(); // Start a new session with generated seed
  }

  inputSeed() {
    if (this.seedForm.valid) {
      this.seed = this.seedForm.value.seed;

      this.joinRoom(); // Attempt to join session with inputted seed
    }
  }

  createRoom() {
    this.sessionService.clearAll(); // Reset previous session data
    this.sessionService.setSessionId(); // Generate new session ID

    const url = `${environment.apiBaseUrl}/api/v1/session/create-room`;

    this.http.post<Movie[]>(url, {
      "seed": this.seed,
      "playerSessionId": this.sessionService.getSessionId(),
      "language": "en-US" // TODO: make this configurable
    }).subscribe({
      next: response => {
        this.initialMovies = response;
        this.sessionService.setMovies(this.initialMovies);
        this.sessionService.setSeed(this.seed);
        this.sessionService.setIndex(0); // Start from beginning
      },
      error: error => {
        console.error('API Error:', error);
      }
    });
  }

  joinRoom() {
    const url = `${environment.apiBaseUrl}/api/v1/session/join-room`;

    this.http.post<Movie[]>(url, {
      "seed": this.seed,
      "playerSessionId": this.sessionService.getSessionId(),
      "language": "en-US" // TODO: make this configurable
    }).subscribe({
      next: response => {
        this.initialMovies = response;
        this.sessionService.setMovies(this.initialMovies);
        this.sessionService.setSeed(this.seed);
        this.sessionService.setIndex(0); // Reset index when joining
      },
      error: error => {
        console.error('API Error:', error);
      }
    });
  }

  playerRejoinCheck() {
    const url = `${environment.apiBaseUrl}/api/v1/session/can-player-rejoin?playerSessionId=${this.sessionService.getSessionId()}`;

    this.http.get<Boolean>(url).subscribe({
      next: response => {
        // Rejoin is only allowed if session has previous index and seed
        this.canPlayerRejoin = response && this.index !== 0 && this.seed !== '';
      },
      error: error => {
        this.canPlayerRejoin = false;
        console.error('API Error:', error);
      }
    })
  }

  rejoinRoom() {
    // Restore session from local storage/service
    this.initialMovies = this.sessionService.getMovies();
    this.seed = this.sessionService.getSeed();
  }

  formatInput(): void {
    // Enforce uppercase alphanumeric input without special characters
    const control = this.seedForm.get('seed');
    if (control) {
      const formatted = (control.value || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
      control.setValue(formatted, { emitEvent: false });
    }
  }
}
