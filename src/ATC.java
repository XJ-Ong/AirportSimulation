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
    private int passengerBoarded = 0;
    private int planesRegistered = 0;
    private int planesServed = 0;

    // Landing and Departing Queues
    private final Queue<Plane> landingQueue = new LinkedList<>();
    private final Queue<Plane> departingQueue = new LinkedList<>();

    public ATC(int gateCount)
    {
        this.gate = new Semaphore(gateCount, true);
        this.gateOccupied = new boolean[gateCount];
    }

    // Landing Queue Management
    public synchronized void requestLanding(Plane plane)
    {
        if(plane.getIsEmergency())
        {
            ((LinkedList<Plane>) landingQueue).addFirst(plane); // Implementing Deque for First-In-Last-Out
        }
        else
        {
            landingQueue.add(plane);
        }
        notifyAll();
    }

    // Departing Queue Management
    public synchronized void requestDeparting(Plane plane)
    {
        departingQueue.add(plane);
        notifyAll();
    }

    public synchronized Plane getNextPlane()
    {
            Plane nextPlane = null;

            // Waiting loop for plane to arrive
            while (landingQueue.isEmpty() && departingQueue.isEmpty() && !isFinished)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }

            // prioritise departing
            if (!departingQueue.isEmpty())
            {
                return departingQueue.poll();
            }
            else
            {
                return landingQueue.poll();
            }
    }

    public void handlePlane(Plane plane)
    {
        // differentiate between landing and departing
        if (plane.getIsLanding())
        {
            // check gate and runway availability
            synchronized (this)
            {
                while (gate.availablePermits() == 0 || runwayOccupied)
                {
                    System.out.println(Thread.currentThread().getName() + ": Runway or all the gates are occupied");
                    try
                    {
                        wait();
                    } catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                System.out.println(Thread.currentThread().getName() + ": Runway is cleared");
            }
            System.out.println(Thread.currentThread().getName() + ": Landing permission granted for Plane-" + plane.getPlaneID());
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
                    } catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
            System.out.println(Thread.currentThread().getName() + ": Taking-off permission granted for Plane-" + plane.getPlaneID());
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
                    throw new RuntimeException(e);
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
                    throw new RuntimeException(e);
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

    public int acquireGate()
    {
        try
        {
            gate.acquire();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        // gets the available gate number
        int gateNumber = 0;
        for (int i = 0; i < gateOccupied.length; i ++)
        {
            if (!gateOccupied[i])
            {
                gateOccupied[i] = true;
                gateNumber = i + 1;
                break;
            }
        }
        return gateNumber;
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

    public synchronized void registerPlane()
    {
        this.planesRegistered ++;
    }

    public synchronized void planeFinished()
    {
        this.planesServed ++;
        if (this.planesServed == this.planesRegistered)
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

    @Override
    public void run()
    {
        System.out.println(Thread.currentThread().getName() + ": ATC has started operating");

        // Handles landing and departing of planes
        while (!isFinished)
        {
            Plane nextPlane = getNextPlane();
            handlePlane(nextPlane);
        }
        System.out.println(Thread.currentThread().getName() + ": All planes are served");

        // sanity check & statistics
        if (!this.sanityCheck())
        {
            System.out.println("Something has gone wrong");
        }
        else
        {
            System.out.println("Minimum plane waiting time  : ");
            System.out.println("Maximum plane waiting time  : ");
            System.out.println("Average plane waiting time  : ");
            System.out.println("Number of Planes Served     : " + planesServed);
            System.out.println("Number of Passengers Boarded: " + passengerBoarded);
        }
    }
}