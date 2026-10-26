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

// Text node written by a timer inside each display element. Flow owns the display's other nodes, so
// the timer only touches its own. Shared across timer instances, so that a timer element created
// again after a detach reuses the node instead of appending a second one.
const displayNodes = new WeakMap();

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
    this._endTimeout = 0;
    this._frame = this._frame.bind(this);
    this._displays = new Set();

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
    this._scheduleEnd();
  }

  disconnectedCallback() {
    cancelAnimationFrame(this._raf);
    this._raf = 0;
    this._scheduleEnd();
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
    this._scheduleEnd();
    if (!running) return;
    this._loop();
    if (pull) this.dispatchEvent(new CustomEvent('simple-timer-sync-request'));
  }

  /** Starts writing the value into the given element. */
  _bindDisplay(target) {
    if (!target) return;
    let node = displayNodes.get(target);
    if (!node || node.parentNode !== target) {
      node = document.createTextNode('');
      target.appendChild(node);
      displayNodes.set(target, node);
    }
    this._displays.add(target);
    this._render();
  }

  /** Stops writing the value into the given element, and removes the text written so far. */
  _unbindDisplay(target) {
    if (!target || !this._displays.delete(target)) return;
    const node = displayNodes.get(target);
    if (node) node.remove();
    displayNodes.delete(target);
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

  /**
   * Arms a timeout for the moment the timer reaches its limit. Frames stop in background tabs, so
   * without it the end would only be noticed when the user returns; timeouts keep firing there,
   * with a precision of about a second.
   */
  _scheduleEnd() {
    clearTimeout(this._endTimeout);
    this._endTimeout = 0;
    if (!this._running || !this.isConnected) return;
    const remaining = Math.ceil(this._limit - this._elapsed());
    this._endTimeout = setTimeout(() => {
      this._endTimeout = 0;
      if (!this._endIfDue()) this._scheduleEnd();
    }, Math.max(remaining, 0));
  }

  /** Ends the timer if it has reached its limit, and returns whether it did. */
  _endIfDue() {
    if (!this._running || this._elapsed() < this._limit) return false;
    this._base = this._limit;
    this._running = false;
    this._scheduleEnd();
    this._render();
    this._fire('simple-timer-end');
    return true;
  }

  _frame() {
    this._raf = 0;
    if (!this._running || this._endIfDue()) return;
    const elapsed = this._elapsed();
    const formatted = this._render();
    this._fire('simple-timer-tick', {elapsed, remaining: this._limit - elapsed, formatted});
    this.dispatchEvent(new CustomEvent('current-time-changed'));
    this._loop();
  }

  _fire(type, detail) {
    this.dispatchEvent(new CustomEvent(type, {bubbles: true, composed: true, detail}));
  }

  _render() {
    const text = formatTime(this._displayed(), this);
    if (this._value.textContent !== text) this._value.textContent = text;
    this._displays.forEach(target => {
      const node = displayNodes.get(target);
      if (!node || !target.isConnected) {
        // Flow replaced or removed the element; the server binds the new one when it attaches
        this._displays.delete(target);
      } else if (node.data !== text) {
        node.data = text;
      }
    });
    return text;
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
