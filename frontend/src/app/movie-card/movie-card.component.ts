import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';
import confetti from 'canvas-confetti';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Movie } from '../models/movie.model';
import { SessionService } from '../session.service';
import { NoTrailerDialogComponent } from './no-trailer-dialog.component';
import { TrailerDialogComponent } from './trailer-dialog.component';


@Component({
  selector: 'app-movie-card',
  imports: [
    CommonModule,

    MatCardModule,
    MatGridListModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
  ],
  templateUrl: './movie-card.component.html',
  styleUrl: './movie-card.component.css'
})
export class MovieCardComponent {
  @Input() movies: Movie[] = [];
  @Input() seed: string = '';

  @Output() return = new EventEmitter<void>();

  index: number = 0;
  showMatchAnimation = false;
  likedMovieIds: number[] = [];
  commonMovies: Movie[] = [];
  displayBigMovieCard: boolean = false;

  constructor(private http: HttpClient, private dialog: MatDialog, private sessionService: SessionService) {
    this.index = this.sessionService.getIndex(); // Restore index from session if available
  }

  skipNext() {
    this.index++;
    this.sessionService.setIndex(this.index);
    this.displayBigMovieCard = false;

    this.getCommonLikes();

    // Fetch more movies every 5 skips — value is hardcoded and flagged as TODO
    if (this.index % 5 == 0) { // TODO: check this value 5
      this.fetchMoreMovies();
    }
  }

  favorite(movieId: number) {
    const url = `${environment.apiBaseUrl}/api/v1/session/add-to-likes`

    this.http.post<Boolean>(url, {
      "seed": this.seed,
      "playerSessionId": this.sessionService.getSessionId(),
      "movieId": movieId
    }).subscribe({
      next: response => {
        this.likedMovieIds.push(movieId);
        this.sessionService.setLiked(this.likedMovieIds);

        // Trigger confetti animation if mutual match (backend returns true)
        if (response === true) {
          this.triggerMatchAnimation(() => {
            this.skipNext();
          });
        } else {
          this.skipNext();
        }
      },
      error: error => {
        console.error('API Error:', error);
        this.skipNext();
      }
    })
  }

  fetchMoreMovies() {
    const url = `${environment.apiBaseUrl}/api/v1/session/fetch-more`;

    this.http.post<Movie[]>(url, {
      "seed": this.seed,
      "playerSessionId": this.sessionService.getSessionId(),
      "language": "en-US" // TODO: make this configurable
    }).subscribe({
      next: response => {
        this.movies.push(...response); // Append new movies to existing list
        this.sessionService.setMovies(this.movies);
      },
      error: error => {
        console.error('API Error:', error);
      }
    })
  }

  openYoutubeTrailerDialog(movieId: number) {
    this.getYoutubeTrailer(movieId).subscribe({
      next: (ytTrailer) => {
        if (ytTrailer != null) {
          // Show trailer if found
          this.dialog.open(TrailerDialogComponent, {
            data: ytTrailer,
            width: '95vw',
            height: '40vh',
          });
        } else {
          // Fallback dialog if no trailer
          this.dialog.open(NoTrailerDialogComponent);
        }
      },
      error: (error) => {
        // Handle 404 separately
        if (error.status === 404) {
          this.dialog.open(NoTrailerDialogComponent);
        } else {
          console.error('API Error:', error);
        }
      }
    });
  }

  getCommonLikes(): void {
    const url = `${environment.apiBaseUrl}/api/v1/session/common-likes?playerSessionId=${this.sessionService.getSessionId()}`;

    this.http.get<number[]>(url).subscribe({
      next: response => {
        this.commonMovies = response
          .map(id => this.movies.find(movie => movie.id === id))//find movies by id
          .filter((m): m is Movie => m !== undefined);//filter out the undefined
      },
      error: error => {
        console.error('API Error:', error);
      }
    });
  }


  getYoutubeTrailer(movieId: number): Observable<string> {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/youtube-trailer/${movieId}/en-US`; // TODO: add lang config

    return this.http.get(url, { responseType: 'text' }); // Returns plain text (YouTube URL)
  }

  back() {
    this.return.emit();
  }

  onClickShowCard() {
    this.displayBigMovieCard = true;
  }

  triggerMatchAnimation(onComplete: () => void) {
    const duration = 2500;
    const animationEnd = Date.now() + duration;
    const defaults = { origin: { y: 0.6 } };

    this.showMatchAnimation = true;

    // Fire confetti every 250ms until duration ends
    const interval = setInterval(() => {
      const timeLeft = animationEnd - Date.now();

      if (timeLeft <= 0) {
        clearInterval(interval);
        this.showMatchAnimation = false;
        onComplete();
      }

      confetti({
        ...defaults,
        particleCount: 100,
        spread: 140,
        startVelocity: 50,
      });
    }, 250);
  }
}

