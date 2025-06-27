import { CdkDragEnd, CdkDragMove, DragDropModule } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';
import confetti from 'canvas-confetti';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { LikedMoviesComponent } from "../liked-movies/liked-movies.component";
import { LocalStorageService } from '../local-storage.service';
import { Movie } from '../models/movie.model';
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

    DragDropModule,

    LikedMoviesComponent
  ],
  templateUrl: './movie-card.component.html',
  styleUrl: './movie-card.component.css'
})
export class MovieCardComponent implements OnInit {
  @Input() movies: Movie[] = [];
  @Input() seed: string = '';

  @Output() return = new EventEmitter<void>();

  index: number = 0;
  showMatchAnimation = false;
  likedMovieIndexes: number[] = [];
  playerLikedMovies: Movie[] = [];
  commonLikedMovies: Movie[] = [];
  displayBigMovieCard: boolean = false;
  dragging = false;
  heartsInterval: any = null;
  skipsInterval: any = null;
  currentConfettiOrigin: { x: number, y: number } | null = null;
  isShowLikedMovies: boolean = false;

  SWIPE_THRESHOLD = 150;

  constructor(private http: HttpClient, private dialog: MatDialog, private localStorageService: LocalStorageService) {
  }

  ngOnInit(): void {
    this.index = this.localStorageService.getIndex(); // Restore index from local storage if available
    this.likedMovieIndexes = this.localStorageService.getLikedIndexes(); // Restore liked movie indexes from local storage if available
    this.setPlayerLikedMovies();
  }

  skipNext() {
    this.index++;
    this.localStorageService.setIndex(this.index);
    this.displayBigMovieCard = false;

    // Fetch more movies every 5 skips — value is hardcoded and flagged as TODO
    if (this.index % 5 == 0) { // TODO: check this value 5
      this.fetchMoreMovies();
    }
  }

  favorite() {
    const url = `${environment.apiBaseUrl}/api/v1/session/add-to-likes`;
    const movieId = this.movies.at(this.index)?.id!;

    this.http.post<Boolean>(url, {
      "seed": this.seed,
      "playerSessionId": this.localStorageService.getSessionId(),
      "movieId": movieId
    }).subscribe({
      next: response => {
        this.likedMovieIndexes.push(this.index);
        this.localStorageService.setLikedIndexes(this.likedMovieIndexes);
        this.setPlayerLikedMovies();

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
      "playerSessionId": this.localStorageService.getSessionId(),
      "language": "en-US" // TODO: make this configurable
    }).subscribe({
      next: response => {
        this.movies.push(...response); // Append new movies to existing list
        this.localStorageService.setMovies(this.movies);
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

  getYoutubeTrailer(movieId: number): Observable<string> {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/youtube-trailer/${movieId}/en-US`; // TODO: add lang config

    return this.http.get(url, { responseType: 'text' }); // Returns plain text (YouTube URL)
  }

  getCommonLikes(): void {
    const url = `${environment.apiBaseUrl}/api/v1/session/common-likes?playerSessionId=${this.localStorageService.getSessionId()}`;

    this.http.get<number[]>(url).subscribe({
      next: response => {
        this.commonLikedMovies = response
          .map(id => this.movies.find(movie => movie.id === id))//find movies by id
          .filter((m): m is Movie => m !== undefined);//filter out the undefined
      },
      error: error => {
        console.error('API Error:', error);
      }
    });
  }

  back() {
    this.return.emit();
  }

  onClickShowCard() {
    if (this.dragging) return;
    this.displayBigMovieCard = true;
  }

  onCardDragMove(event: CdkDragMove) {
    this.dragging = true;
    const x = event.distance.x;

    // Use the actual pointer position for confetti origin
    const pointerX = event.pointerPosition.x;
    const pointerY = event.pointerPosition.y;

    // Save latest pointer position as confetti origin (normalized 0-1)
    this.currentConfettiOrigin = {
      x: Math.min(Math.max(pointerX / window.innerWidth, 0), 1),
      y: Math.min(Math.max(pointerY / window.innerHeight, 0), 1)
    };

    // Start hearts animation if swiping right and not already running
    if (x > this.SWIPE_THRESHOLD && !this.heartsInterval) {
      this.heartsInterval = setInterval(() => {
        var scalar = 10;
        var heart = confetti.shapeFromText({ text: '❤️', scalar });
        confetti({
          particleCount: 1,
          spread: 90,
          flat: true,
          origin: this.currentConfettiOrigin ?? { x: 0.5, y: 0.5 },
          shapes: [heart],
          scalar
        });
      }, 250);
    }

    // Start skips animation if swiping left and not already running
    if (x < -this.SWIPE_THRESHOLD && !this.skipsInterval) {
      this.skipsInterval = setInterval(() => {
        var scalar = 10;
        var cross = confetti.shapeFromText({ text: '❌', scalar });
        confetti({
          particleCount: 1,
          spread: 90,
          flat: true,
          origin: this.currentConfettiOrigin ?? { x: 0.5, y: 0.5 },
          shapes: [cross],
          scalar
        });
      }, 250);
    }

    // Stop hearts animation if user drags back to center or left
    if (x <= this.SWIPE_THRESHOLD && this.heartsInterval) {
      clearInterval(this.heartsInterval);
      this.heartsInterval = null;
      confetti.reset();
    }

    // Stop skips animation if user drags back to center or right
    if (x >= -this.SWIPE_THRESHOLD && this.skipsInterval) {
      clearInterval(this.skipsInterval);
      this.skipsInterval = null;
      confetti.reset();
    }
  }

  onCardDragEnded(event: CdkDragEnd) {
    const x = event.distance.x;
    if (x > this.SWIPE_THRESHOLD) {
      // Swiped right (like)
      this.favorite();
    } else if (x < -this.SWIPE_THRESHOLD) {
      // Swiped left (skip)
      this.skipNext();
    }
    // Reset card position
    event.source._dragRef.reset();

    // Stop hearts animation if running
    if (this.heartsInterval) {
      clearInterval(this.heartsInterval);
      this.heartsInterval = null;
      confetti.reset();
    }

    if (this.skipsInterval) {
      clearInterval(this.skipsInterval);
      this.skipsInterval = null;
      confetti.reset();
    }

    setTimeout(() => this.dragging = false, 0); // Reset after event loop
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

  showLikedMovies() {
    this.getCommonLikes();
    this.isShowLikedMovies = true;
  }

  onReturn() {
    this.isShowLikedMovies = false;
  }

  setPlayerLikedMovies() {
    this.playerLikedMovies = this.likedMovieIndexes
      .map(index => this.movies.at(index)!);
  }
}

