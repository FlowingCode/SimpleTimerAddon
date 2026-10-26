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

const pad = n => (n < 10 ? '0' : '') + n;

/**
 * Formats a time value given in milliseconds, e.g. "1:02:03.45".
 * Negative values are shown as zero. Fractions are truncated to hundredths.
 */
export function formatTime(millis, {fractions, minutes, hours, doubleDigitHours}) {
  const ms = millis > 0 ? Math.floor(millis) : 0;
  const showMinutes = minutes || hours;
  let s = Math.floor(ms / 1000);
  let m = Math.floor(s / 60);
  const h = Math.floor(m / 60);
  if (hours) m %= 60;
  if (showMinutes) s %= 60;

  let text = '';
  if (hours) text += (doubleDigitHours ? pad(h) : h) + ':';
  if (showMinutes) text += pad(m) + ':';
  text += pad(s);
  if (fractions) text += '.' + pad(Math.floor((ms % 1000) / 10));
  return text;
}

const FORMAT_PROPERTIES = ['fractions', 'minutes', 'hours', 'doubleDigitHours'];

/**
 * Renders a timer whose state is owned by the server.
 *
 * The server pushes the elapsed time as a duration, never as an instant, because the server and
 * browser clocks are not the same clock. The element anchors that duration to its own
 * performance.now() when it arrives, and derives the current value on every frame.
 */
class SimpleTimer extends HTMLElement {

  constructor() {
    super();
    this._fractions = true;
    this._minutes = false;
    this._hours = false;
    this._doubleDigitHours = false;

    this._base = 0;       // elapsed milliseconds at _origin
    this._origin = 0;     // performance.now() when _base was received
    this._running = false;
    this._limit = 0;
    this._countUp = false;
    this._raf = 0;
    this._frame = this._frame.bind(this);

    this.attachShadow({mode: 'open'}).innerHTML = '<span part="value"></span>';
    this._value = this.shadowRoot.firstChild;
  }

  connectedCallback() {
    // Flow may set properties before the element is upgraded; re-apply them through the setters.
    FORMAT_PROPERTIES.forEach(name => {
      if (Object.prototype.hasOwnProperty.call(this, name)) {
        const value = this[name];
        delete this[name];
        this[name] = value;
      }
    });
    this._render();
    if (this._running) this._loop();
  }

  disconnectedCallback() {
    cancelAnimationFrame(this._raf);
    this._raf = 0;
  }

  /** The displayed value in seconds. Kept for the deprecated currentTime property listener. */
  get currentTime() {
    return this._displayed() / 1000;
  }

  /**
   * Applies the clock state pushed by the server.
   *
   * When pull is set and the timer is running, the element asks for the state again. A push can be
   * deferred while the element is hidden, which would leave a stale origin; the answer to the pull
   * is computed when it is sent, so it always lands on the right time.
   */
  _sync(elapsed, running, limit, countUp, pull) {
    this._base = elapsed;
    this._origin = performance.now();
    this._limit = limit;
    this._countUp = countUp;
    this._running = running;
    this._render();
    if (!running) return;
    this._loop();
    if (pull) this.dispatchEvent(new CustomEvent('simple-timer-sync-request'));
  }

  _elapsed() {
    const elapsed = this._base + (this._running ? performance.now() - this._origin : 0);
    return Math.min(elapsed, this._limit);
  }

  _displayed() {
    const elapsed = this._elapsed();
    return this._countUp ? elapsed : this._limit - elapsed;
  }

  _loop() {
    if (!this._raf && this.isConnected) this._raf = requestAnimationFrame(this._frame);
  }

  _frame() {
    this._raf = 0;
    if (!this._running) return;
    if (this._elapsed() >= this._limit) {
      this._base = this._limit;
      this._running = false;
      this._render();
      this._fire('simple-timer-end');
      return;
    }
    this._render();
    this.dispatchEvent(new CustomEvent('current-time-changed'));
    this._loop();
  }

  _fire(type, detail) {
    this.dispatchEvent(new CustomEvent(type, {bubbles: true, composed: true, detail}));
  }

  _render() {
    const text = formatTime(this._displayed(), this);
    if (this._value.textContent !== text) this._value.textContent = text;
  }
}

FORMAT_PROPERTIES.forEach(name => {
  Object.defineProperty(SimpleTimer.prototype, name, {
    get() {
      return this['_' + name];
    },
    set(value) {
      this['_' + name] = !!value;
      this._render();
    },
  });
});

customElements.define('simple-timer', SimpleTimer);
