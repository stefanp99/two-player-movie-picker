import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
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
export class MovieCardComponent implements OnInit {
  @Input() movies: Movie[] = [];
  @Input() seed: string = '';
  index: number = 0;
  showMatchAnimation = false;

  constructor(private http: HttpClient, private dialog: MatDialog, private sessionService: SessionService) {
  }

  ngOnInit(): void {
    if (this.sessionService.getIndex() == -1) {
      this.sessionService.setIndex(this.index);
      this.sessionService.setMovies(this.movies);
      this.sessionService.setSeed(this.seed);
    }
    else {
      this.index = this.sessionService.getIndex();
      this.movies = this.sessionService.getMovies();
      this.seed = this.sessionService.getSeed();
    }
  }

  skipNext() {
    this.index++;
    this.sessionService.setIndex(this.index);
    if (this.index % 5 == 0) {//TODO: check this value 5
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
        console.log('Response: ' + response);
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
      "language": "en-US"//TODO: make this configurable
    }).subscribe({
      next: response => {
        this.movies.push(...response);
        this.sessionService.setMovies(this.movies);
        console.log('New List:', this.movies);
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
          this.dialog.open(TrailerDialogComponent, {
            data: ytTrailer,
            width: '50vw',
            height: '50vh',
          });
        } else {
          this.dialog.open(NoTrailerDialogComponent);
        }
      },
      error: (error) => {
        if (error.status === 404) {
          this.dialog.open(NoTrailerDialogComponent);
        } else {
          console.error('API Error:', error);
        }
      }
    });
  }

  getYoutubeTrailer(movieId: number): Observable<string> {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/youtube-trailer/${movieId}/en-US`;//TODO: add lang config

    return this.http.get(url, { responseType: 'text' });
  }

  triggerMatchAnimation(onComplete: () => void) {
    const duration = 2500;
    const animationEnd = Date.now() + duration;
    const defaults = { origin: { y: 0.6 } };

    this.showMatchAnimation = true;

    // Fire confetti bursts continuously during the animation
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
    }, 250); // every 250ms
  }


}
