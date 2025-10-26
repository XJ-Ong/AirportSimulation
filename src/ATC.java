import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class ATC implements Runnable
{
    private boolean runwayOccupied = false;
    private boolean fuelTruckOccupied = false;
    private final Semaphore gate;
    private boolean[] gateOccupied;
    private boolean isFinished = false;

    // statistics
    private int passengerBoarded = 0;
    private int expectedPlanes;
    private int planesServed = 0;
    private long totalWaitingTime = 0;
    private long minWaitingTime = Long.MAX_VALUE;
    private long maxWaitingTime = Long.MIN_VALUE;

    // landing and departing Queues
    private final Queue<Plane> landingQueue = new LinkedList<>();
    private final Queue<Plane> departingQueue = new LinkedList<>();

    public ATC(int gateCount, int expectedPlanes)
    {
        this.gate = new Semaphore(gateCount, true);
        this.gateOccupied = new boolean[gateCount];
        this.expectedPlanes = expectedPlanes;
    }

    // landing queue management
    public synchronized void requestLanding(Plane plane)
    {
        if(plane.getIsEmergency())
        {
            ((LinkedList<Plane>) landingQueue).addFirst(plane); // implementing Deque to cut the queue
        }
        else
        {
            landingQueue.add(plane);
        }
        notifyAll();
    }

    // departing queue management
    public synchronized void requestDeparting(Plane plane)
    {
        departingQueue.add(plane);
        notifyAll();
    }

    public synchronized Plane getNextPlane()
    {
        // waiting loop for plane to arrive
        while (landingQueue.isEmpty() && departingQueue.isEmpty() && !isFinished)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        // check if all planes are served
        if (isFinished && landingQueue.isEmpty() && departingQueue.isEmpty())
        {
            return null;
        }

        // prioritise departing
        if (!departingQueue.isEmpty())
        {
            return departingQueue.poll();
        }

        if (!landingQueue.isEmpty() && this.gate.availablePermits() > 0)
        {
            return landingQueue.poll();
        }
        return null;
    }

    public void handlePlane(Plane plane)
    {
        if (plane == null)
        {
            return;
        }

        // differentiate between landing and departing
        if (plane.getIsLanding())
        {
            // check gate and runway availability
            synchronized (this)
            {
                while (gate.availablePermits() == 0 || runwayOccupied)
                {
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                // acquire gate
                try
                {
                    gate.acquire();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return;
                }

                // gets the available gate number
                int gateNumber = -1;
                synchronized (this)
                {
                    for (int i = 0; i < gateOccupied.length; i++)
                    {
                        if (!gateOccupied[i])
                        {
                            gateOccupied[i] = true;
                            gateNumber = i + 1;
                            break;
                        }
                    }
                }

                if (gateNumber != -1)
                {
                    plane.setGateNumber(gateNumber);
                    System.out.println(Thread.currentThread().getName() + ": Landing Permission granted for Plane-" + plane.getPlaneID());
                    System.out.println(Thread.currentThread().getName() + ": Gate-" + gateNumber + " is assigned for Plane-" + plane.getPlaneID());
                }
                else
                {
                    gate.release();
                }
                notifyAll();
            }
        }
        else
        {
            synchronized (this)
            {
                while (runwayOccupied)
                {
                    System.out.println(Thread.currentThread().getName() + ": Runway is occupied");
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                System.out.println(Thread.currentThread().getName() + ": Taking-off permission granted for Plane-" + plane.getPlaneID());
            }
        }
        synchronized (plane)
        {
            plane.notify();
        }
    }

    public void acquireRunway()
    {
        synchronized (this)
        {
            while (runwayOccupied)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            runwayOccupied = true;
            System.out.println(Thread.currentThread().getName() + ": Acquiring Runway");
        }
    }

    public void releaseRunway()
    {
        synchronized (this)
        {
            runwayOccupied = false;
            System.out.println(Thread.currentThread().getName() + ": Releasing Runway");
            notifyAll();
        }
    }

    public void acquireFuelTruck()
    {
        synchronized (this)
        {
            while (fuelTruckOccupied)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            fuelTruckOccupied = true;
        }
    }

    public void releaseFuelTruck()
    {
        synchronized (this)
        {
            fuelTruckOccupied = false;
            notifyAll();
        }
    }

    public void releaseGate(int gateNumber)
    {
        synchronized (this)
        {
            gateOccupied[gateNumber - 1] = false;
            notifyAll();
        }
        gate.release();
    }

    public void updatePassengersBoarded(int count)
    {
        synchronized (this)
        {
            this.passengerBoarded += count;
        }
    }


    public synchronized void planeFinished()
    {
        this.planesServed ++;
        if (this.planesServed == this.expectedPlanes)
        {
            this.isFinished = true;
            notifyAll();
        }
    }

    public boolean sanityCheck()
    {
        if (gate.availablePermits() == gateOccupied.length && !runwayOccupied)
        {
            System.out.println("Sanity check completed, all gates are empty, displaying statistics...");
            return true;
        }
        else
        {
            return false;
        }
    }

    public void reportWaitingTime(long waitingTime)
    {
        this.totalWaitingTime += waitingTime;
        if (waitingTime < minWaitingTime)
        {
            this.minWaitingTime = waitingTime;
        }
        if (waitingTime > maxWaitingTime)
        {
            this.maxWaitingTime = waitingTime;
        }
    }


    @Override
    public void run()
    {
        System.out.println(Thread.currentThread().getName() + ": ATC has started operating");

        // landing and departing of planes
        while (!isFinished)
        {
            Plane nextPlane = getNextPlane();
            if (nextPlane == null)
            {
                continue;
            }
            handlePlane(nextPlane);
        }
        System.out.println(Thread.currentThread().getName() + ": All planes are served");

        // sanity check & statistics
        if (!this.sanityCheck())
        {
            System.out.println("Something had gone wrong");
        }
        else
        {
            System.out.println("Minimum plane waiting time  : " + this.minWaitingTime + " ms");
            System.out.println("Maximum plane waiting time  : " + this.maxWaitingTime + " ms");
            System.out.println("Average plane waiting time  : " + (this.totalWaitingTime / this.planesServed) + " ms");
            System.out.println("Number of Planes Served     : " + this.planesServed);
            System.out.println("Number of Passengers Boarded: " + this.passengerBoarded);
        }
    }
}