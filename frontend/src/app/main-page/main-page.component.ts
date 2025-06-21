import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
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
    MatDividerModule,

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
      seed: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{4}$/)]]
    });
  }

  ngOnInit(): void {
    this.index = this.sessionService.getIndex();
    this.seed = this.sessionService.getSeed();
    this.playerRejoinCheck()
  }

  generateSeed() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = '';
    for (let i = 0; i < 4; i++) {
      const randomIndex = Math.floor(Math.random() * chars.length);
      result += chars[randomIndex];
    }
    this.seed = result;

    this.createRoom();
  }

  inputSeed() {
    if (this.seedForm.valid) {
      this.seed = this.seedForm.value.seed;

      this.joinRoom();
    }
  }

  createRoom() {
    this.sessionService.clearAll();
    this.sessionService.setSessionId();

    const url = `${environment.apiBaseUrl}/api/v1/session/create-room`;

    this.http.post<Movie[]>(url, {
      "seed": this.seed,
      "playerSessionId": this.sessionService.getSessionId(),
      "language": "en-US"//TODO: make this configurable
    }).subscribe({
      next: response => {
        this.initialMovies = response;
        this.sessionService.setMovies(this.initialMovies);
        this.sessionService.setSeed(this.seed);
        this.sessionService.setIndex(0);
        console.log('API Response:', this.initialMovies);
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
      "language": "en-US"//TODO: make this configurable
    }).subscribe({
      next: response => {
        this.initialMovies = response;
        this.sessionService.setMovies(this.initialMovies);
        this.sessionService.setSeed(this.seed);
        this.sessionService.setIndex(0);
        console.log('API Response:', this.initialMovies);
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
        this.canPlayerRejoin = response && this.index !== 0 && this.seed !== '';
        console.log('Can player rejoin? ' + this.canPlayerRejoin);
      },
      error: error => {
        this.canPlayerRejoin = false;
        console.error('API Error:', error);
      }
    })
  }

  rejoinRoom() {
    this.initialMovies = this.sessionService.getMovies();
    this.seed = this.sessionService.getSeed();
  }

  formatInput(): void {
    const control = this.seedForm.get('seed');
    if (control) {
      const formatted = (control.value || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
      control.setValue(formatted, { emitEvent: false });
    }
  }
}
