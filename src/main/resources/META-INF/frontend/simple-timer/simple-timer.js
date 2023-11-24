/**
@license MIT

Copyright (c) 2016 Anson Chung

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
//import '@polymer/polymer/polymer-legacy.js';

import {Polymer} from '@polymer/polymer/lib/legacy/polymer-fn.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

Polymer({
	  _template: html`
		<style>
		  :host {
			display: block;
		  }
		</style>
		{{_formattedTime}}
	  `,

      is: 'simple-timer',

      properties: {
        /**
        * Start time for the timer in seconds
        * @default 60
        */
        startTime: {
          type: Number,
          value: 60
        },
        /**
        * Current time of the timer, in seconds
        */
        currentTime: {
          type: Number,
          notify: true
        },
        /**
        * True if the timer is currently running
        * @default false
        */
        isRunning: {
          type: Boolean,
          reflectToAttribute: true,
          notify: true,
          value: false
        },
        /**
        * Set to true to have timer count up
        * @default false
        */
        countUp: {
          type: Boolean,
          value: false
        },
        /**
         * Set to false to hide fractional seconds
         * @default true
         */
        fractions: {
        	type: Boolean,
        	value: true
        },
        /**
         * Set to true to show minutes
         * @default false
         */
        minutes: {
        	type: Boolean,
        	value: false
        },
        /**
         * Set to true to show hours, and minutes
         * @default false
         */
        hours: {
        	type: Boolean,
        	value: false
        },
        /**
         * Use two digits for hours
         * @default false
         */
        doubleDigitHours: {
        	type: Boolean,
        	value: false
        },
        /**
        * Time the timer has spent running since it was started
        */
        _elapsedTime: {
          type: Number,
          value: 0
        },

        _formattedTime: {
          type: String,
          value: '0'
        }
      },

      ready: function() {
        if (this.countUp) {
          this.set('currentTime', 0);
          this.set('_formattedTime', '0');
        } else {
          this.set('currentTime', this.startTime);
          this.set('_formattedTime', this.startTime.toString());
        }
      },

      start: function() {
        if ((this.currentTime <= 0 && !this.countUp) 
            || (this.currentTime >= this.startTime && this.countUp) ) {
          // timer is over
          this.currentTime = this.countUp ? this.startTime : 0;
        }
        
        if (!this.startTime || this.isRunning) {
          this.pause();
          return;
        }
        this._elapsed = performance.now()/1000;
        this.isRunning = true;
        window.requestAnimationFrame(this._decreaseTimer.bind(this));
      },

      pause: function() {
        this.isRunning = false;
      },

      _decreaseTimer: function(timestamp) {
        if (!this.isRunning) {
          return;
        }
        if ((this.currentTime <= 0 && !this.countUp) 
            || (this.currentTime >= this.startTime && this.countUp) ) {
          // timer is over
          this.currentTime = this.countUp ? this.startTime : 0;
          this.pause();
          this.fire('simple-timer-end');
          return;
        }

        var now = timestamp/1000;
        // Compute the relative progress based on the time spent running
        var progress = now - this._elapsed;
        this.currentTime = this.countUp ? this.currentTime + progress : this.currentTime - progress;
        this._formattedTime = this._formatTime(this.currentTime);
        this._elapsed = now;
        window.requestAnimationFrame(this._decreaseTimer.bind(this));
      },

      _formatTime: function(time) {
        var timeString = time.toString().split('.');
        if (timeString[0].indexOf('-') === 0) {
        	return 0;
        }
        var seconds = timeString[0];
        var minutes = seconds / 60 | 0;
        var hours = minutes / 60 | 0;
        
        minutes = this.hours ? minutes % 60 : minutes;
        minutes = minutes < 10 ? '0' + minutes : minutes;
        
        seconds = this.minutes || this.hours ? seconds % 60 : seconds;
        seconds = seconds < 10 ? '0' + seconds : seconds;
        
        if(this.hours && this.doubleDigitHours) {
			hours = hours.toString().padStart(2, '0');
		}
        return (this.hours ? hours + ':' : '') + (this.minutes || this.hours ? minutes + ':' : '') + seconds + (this.fractions ? ('.' + timeString[1].substring(0,2)) : '') 
      }
    });