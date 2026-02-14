package CNPROJECT.SERVER;

public class Complexj {

    public double re;
    public double im;

    public Complexj(double r, double i) {
        re = r;
        im = i;
    }

    public Complex add(Complex c) {
        double real = this.re + c.re;
        double imag = this.im + c.im;
        return new Complex(real, imag);
    }

    public Complex sub(Complex c) {
        double real = this.re - c.re;
        double imag = this.im - c.im;
        return new Complex(real, imag);
    }

    public Complex mul(Complex c) {
        double real = (this.re * c.re) - (this.im * c.im);
        double imag = (this.re * c.im) + (this.im * c.re);
        return new Complex(real, imag);
    }

    public Complex div(Complex c) {
        double denom = (c.re * c.re) + (c.im * c.im);
        double real = (this.re * c.re + this.im * c.im) / denom;
        double imag = (this.im * c.re - this.re * c.im) / denom;
        return new Complex(real, imag);
    }

    public Complex exp() {
        double e = Math.exp(this.re);
        double real = e * Math.cos(this.im);
        double imag = e * Math.sin(this.im);
        return new Complex(real, imag);
    }

    public Complex log() {
        double r = Math.sqrt(this.re * this.re + this.im * this.im);
        double theta = Math.atan2(this.im, this.re);
        return new Complex(Math.log(r), theta);
    }

    public Complex pow(Complex c) {
        Complex logz = this.log();
        Complex mult = c.mul(logz);
        return mult.exp();
    }

    public String toString() {
        return this.re + " + " + this.im + "i";
    }
}