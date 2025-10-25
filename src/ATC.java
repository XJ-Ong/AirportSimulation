import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class ATC implements Runnable
{
    private boolean runwayOccupied = false;
    private boolean fuelTruckOccupied = false;
    private final Semaphore gate;

    // Landing and Departing Queues
    private final Queue<Plane> landingQueue = new LinkedList<>();
    private final Queue<Plane> departingQueue = new LinkedList<>();

    public ATC(int gateCount)
    {
        this.gate = new Semaphore(gateCount, true);
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
        while (true)
        {
            Plane nextPlane = null;

            // Waiting loop for plane to arrive
            while (landingQueue.isEmpty() && departingQueue.isEmpty())
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

            if (!landingQueue.isEmpty())
            {
                return landingQueue.poll();
            }
            else
            {
                return departingQueue.poll();
            }
        }
    }

    public void handlePlane(Plane plane)
    {
        // wait for runway to be free
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
        }

        // differentiate between landing and departing
        if (plane.getIsLanding())
        {
            System.out.println(Thread.currentThread().getName() + ": Landing permission granted for Plane-" + plane.getPlaneID());
        }
        else
        {
            System.out.println(Thread.currentThread().getName() + ": Taking-off permission granted for Plane-" + plane.getPlaneID());
        }

        synchronized (plane)
        {
            plane.notify();
        }
    }

    @Override
    public void run()
    {
        System.out.println(Thread.currentThread().getName() + ": ATC has started operating");

        // Handles landing and departing of planes
        while (true)
        {
            Plane nextPlane = getNextPlane();
            handlePlane(nextPlane);
        }

    }

}