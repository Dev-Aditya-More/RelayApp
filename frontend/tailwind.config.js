/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        ink: { DEFAULT: '#14172B', 800: '#1D2140', 700: '#2A2F57', 500: '#5B6190', 300: '#A7ABCB', 100: '#E7E8F3' },
        baton: { DEFAULT: '#FF5B2E', 600: '#E8451A', 100: '#FFE6DD', 50: '#FFF3EE' },
        paper: { DEFAULT: '#FAF6EF', 200: '#F2EBDD' },
      },
      fontFamily: {
        display: ['"Bricolage Grotesque"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        sans: ['"DM Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      boxShadow: { card: '0 1px 0 rgba(20,23,43,.04), 0 8px 24px -12px rgba(20,23,43,.18)' },
    },
  },
  plugins: [],
}
