package CNPROJECT.SERVER;

// Complex.java
public final class Complex {
    public final double re;
    public final double im;

    public Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public Complex add(Complex o) {
        return new Complex(this.re + o.re, this.im + o.im);
    }

    public Complex sub(Complex o) {
        return new Complex(this.re - o.re, this.im - o.im);
    }

    public Complex mul(Complex o) {
        return new Complex(re * o.re - im * o.im, re * o.im + im * o.re);
    }

    public Complex div(Complex o) {
        double denom = o.re * o.re + o.im * o.im;
        return new Complex((re * o.re + im * o.im) / denom, (im * o.re - re * o.im) / denom);
    }

    public double abs() {
        return Math.hypot(re, im);
    }

    public double arg() {
        return Math.atan2(im, re);
    }

    // exp(z) = e^x (cos y + i sin y) for z = x + i y
    public Complex exp() {
        double ex = Math.exp(re);
        return new Complex(ex * Math.cos(im), ex * Math.sin(im));
    }

    // principal log: ln r + i theta, theta = atan2(im, re)
    public Complex log() {
        double r = abs();
        double theta = arg(); // principal branch (-pi, pi]
        return new Complex(Math.log(r), theta);
    }

    // z1 ^ z2 = exp(z2 * log(z1)) (principal branch)
    public Complex pow(Complex exponent) {
        Complex l = this.log();
        Complex mult = exponent.mul(l);
        return mult.exp();
    }

    @Override
    public String toString() {
        return String.format("%.12f%+.12fi", re, im);
    }
}
