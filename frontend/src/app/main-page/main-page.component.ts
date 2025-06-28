import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { environment } from '../../environments/environment';
import { LocalStorageService } from '../local-storage.service';
import { Genre } from '../models/genre.model';
import { Movie } from '../models/movie.model';
import { WatchProvider } from '../models/watch-provider.model';
import { MovieCardComponent } from '../movie-card/movie-card.component';

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
    MatCardModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatChipsModule,

    MovieCardComponent,
  ],
  templateUrl: './main-page.component.html',
  styleUrl: './main-page.component.css'
})
export class MainPageComponent implements OnInit {
  seed: string = '';
  index: number = 0;
  canPlayerRejoin: boolean = false;
  initialMovies: Movie[] = [];
  roomExists: boolean | undefined;
  isFetchingMovies: boolean = false;
  genres: Genre[] = [];
  selectedGenresIds: number[] = [];
  watchProviders: WatchProvider[] = [];
  selectedWatchProviderIds: number[] = [];
  locale: string = 'en-US';
  country: string = 'US';

  inputSeedForm: FormGroup;

  constructor(private fb: FormBuilder, private http: HttpClient, private localStorageService: LocalStorageService) {
    this.inputSeedForm = this.fb.group({
      seed: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{4}$/)]] // 4-character alphanumeric seed
    });
  }

  ngOnInit(): void {
    this.index = this.localStorageService.getIndex(); // Restore session index
    this.seed = this.localStorageService.getSeed(); // Restore session seed
    this.playerRejoinCheck(); // Determine if rejoin is allowed
    this.getGenres(); // Get TMDB available genres
    this.getWatchProviders(); // Get TMDB Watch Providers
    this.locale = navigator.language || (navigator.languages && navigator.languages[0]) || 'en-US';
    this.country = this.locale.includes('-') ? this.locale.split('-')[1] : 'US';
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
    if (this.inputSeedForm.valid) {
      this.seed = this.inputSeedForm.value.seed;

      this.joinRoom(); // Attempt to join session with inputted seed
    }
  }

  createRoom() {
    this.localStorageService.clearAll(); // Reset previous session data
    this.localStorageService.setSessionId(); // Generate new session ID
    this.isFetchingMovies = true;

    const url = `${environment.apiBaseUrl}/api/v1/session/create-room`;

    this.http.post<Movie[]>(url, {
      "seed": this.seed,
      "playerSessionId": this.localStorageService.getSessionId(),
      "language": "en-US", // TODO: make this configurable
      "genres": this.selectedGenresIds,
      "watchRegion": this.country,
      "watchProviders": this.selectedWatchProviderIds
    }).subscribe({
      next: response => {
        this.initialMovies = response;
        this.localStorageService.setMovies(this.initialMovies);
        this.localStorageService.setSeed(this.seed);
        this.localStorageService.setIndex(0); // Start from beginning
        this.isFetchingMovies = false;
      },
      error: error => {
        console.error('API Error:', error);
      }
    });
  }

  joinRoom() {
    const url = `${environment.apiBaseUrl}/api/v1/session/join-room`;
    this.isFetchingMovies = true;

    this.http.post<Movie[]>(url, {
      "seed": this.seed,
      "playerSessionId": this.localStorageService.getSessionId(),
      "language": "en-US" // TODO: make this configurable
    }).subscribe({
      next: response => {
        this.initialMovies = response;
        this.localStorageService.setMovies(this.initialMovies);
        this.localStorageService.setSeed(this.seed);
        this.localStorageService.setIndex(0); // Reset index when joining
        this.localStorageService.setLikedIndexes([]);
        this.isFetchingMovies = false;
      },
      error: error => {
        console.error('API Error:', error);
      }
    });
  }

  playerRejoinCheck() {
    const url = `${environment.apiBaseUrl}/api/v1/session/can-player-rejoin?playerSessionId=${this.localStorageService.getSessionId()}`;

    this.http.get<boolean>(url).subscribe({
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
    this.initialMovies = this.localStorageService.getMovies();
    this.seed = this.localStorageService.getSeed();
  }

  formatInputAndCheckRoomExists() {
    // Enforce uppercase alphanumeric input without special characters
    const control = this.inputSeedForm.get('seed');
    if (control) {
      const formatted = (control.value || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
      control.setValue(formatted, { emitEvent: false });
      if (formatted.length == 4) {
        const url = `${environment.apiBaseUrl}/api/v1/session/room-exists?seed=${formatted}`;

        this.http.get<boolean>(url).subscribe({
          next: response => {
            this.roomExists = response;
          },
          error: error => {
            this.roomExists = false;
            console.error('API Error:', error);
          }
        });
      }
      else {
        this.roomExists = undefined;
      }
    }
  }

  getGenres() {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/genres`;
    this.http.get<Genre[]>(url).subscribe({
      next: response => {
        this.genres = response;
      },
      error: error => {
        console.error('API Error:', error);
      }
    })
  }

  getWatchProviders() {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/watch-providers/${this.country}/en-US`;
    this.http.get<WatchProvider[]>(url).subscribe({
      next: response => {
        this.watchProviders = response;
      },
      error: error => {
        console.error('API Error:', error);
      }
    });
  }

  toggleGenre(id: number) {
    const index = this.selectedGenresIds.indexOf(id);
    if (index > -1) {
      this.selectedGenresIds.splice(index, 1);
    } else {
      this.selectedGenresIds.push(id);
    }
  }

  toggleWatchProvider(providerId: number) {
    const index = this.selectedWatchProviderIds.indexOf(providerId);
    if (index > -1) {
      this.selectedWatchProviderIds.splice(index, 1);
    } else {
      this.selectedWatchProviderIds.push(providerId);
    }
  }

  onCountryChange(country: any) {
    console.log(country);
  }

  onReturn() {
    this.initialMovies = [];
    this.inputSeedForm.reset();
    this.canPlayerRejoin = true;
  }
}
