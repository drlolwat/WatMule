package org.lolwat; /**
 * WindMouse from SMART by Benland100
 * Copyright to Benland100, (Benjamin J. Land)
 *
 * Prepped for DreamBot 3
 **/

import org.dreambot.api.input.Mouse;
import org.dreambot.api.input.mouse.algorithm.MouseMovementAlgorithm;
import org.dreambot.api.input.mouse.destination.AbstractMouseDestination;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.input.mouse.MouseSettings;

import java.awt.*;

import static java.lang.Thread.sleep;

public class BezierMouse implements MouseMovementAlgorithm {
    private boolean isMoving = false;
    @Override
    public boolean handleMovement(AbstractMouseDestination abstractMouseDestination) {
        if (isMoving) {
            return false;
        }

        isMoving = true;
        Point suitPos = abstractMouseDestination.getSuitablePoint();
        bezierMouse(suitPos);
        isMoving = false;

        return distance(Mouse.getPosition(), suitPos) < 2;
    }

    public void bezierMouse(Point point) {
        Point curPos = Mouse.getPosition();
        double initialDistance = distance(point, curPos);
        double currentDistance = initialDistance;

        while (currentDistance / initialDistance > 0.05 && Calculations.random(1) == 2) { // Loop until distance ratio is below 5% or counter reaches 10
            Point rp = randomPoint(curPos, point);
            mouseBezier(curPos, rp);
            sleep(1, 100);
            curPos = Mouse.getPosition();
            currentDistance = distance(point, curPos);
        }
        mouseBezier(curPos, point);
    }

    private void mouseBezier(Point startPos, Point endPos) {
        Point controlPoint = randomPoint(startPos, endPos);
        int steps = Calculations.random(5, 25);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Point pointOnCurve = calculateBezierPoint(t, startPos, controlPoint, endPos);
            sleep(34);
            Mouse.hop(pointOnCurve);
        }
    }

    private static Point calculateBezierPoint(double t, Point p0, Point p1, Point p2) {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;

        Point p = new Point();
        p.x = (int) (uu * p0.x + 2 * u * t * p1.x + tt * p2.x);
        p.y = (int) (uu * p0.y + 2 * u * t * p1.y + tt * p2.y);

        return p;
    }

    private static Point randomPoint(Point a, Point b) {
        int x = a.x + (int) (Math.random() * (b.x - a.x));
        int y = a.y + (int) (Math.random() * (b.y - a.y));
        return new Point(x, y);
    }

    private static double distance(Point a, Point b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    private void sleep(int min, int max) {
        try {
            Thread.sleep(Calculations.random(min, max));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}