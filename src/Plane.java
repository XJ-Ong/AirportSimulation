public class Plane implements Runnable
{
    private final int planeID;
    private int passengerCount;
    private final ATC atc;
    private boolean isLanding = true;
    private boolean isEmergency;

    public Plane(int planeID, int passengerCount, ATC atc)
    {
        this.planeID = planeID;
        this.passengerCount = passengerCount;
        this.atc = atc;
        this.isEmergency = planeID == 4;
        atc.registerPlane();
    }

    public int getPlaneID()
    {
        return this.planeID;
    }

    public boolean getIsEmergency()
    {
        return this.isEmergency;
    }

    public boolean getIsLanding()
    {
        return this.isLanding;
    }

    public void setPassengerCount(int passengerCount)
    {
        this.passengerCount = passengerCount;
    }

    public void setLanding (boolean bool)
    {
        this.isLanding = bool;
    }

    @Override
    public void run()
    {
        synchronized (this)
        {
            if (getIsEmergency())
            {
                System.out.println(Thread.currentThread().getName() + ": Requesting emergency landing!");
            } else
            {
                System.out.println(Thread.currentThread().getName() + ": Requesting landing...");
            }

            atc.requestLanding(this);
            try
            {
                wait();
            } catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        // landing on runway
        atc.acquireRunway();
        System.out.println(Thread.currentThread().getName() + ": Landing on runway...");
        atc.releaseRunway();

        // coasting to gate
        int gateNumber = atc.acquireGate();
        System.out.println(Thread.currentThread().getName() + ": Docked at Gate-" + gateNumber);

        System.out.println(Thread.currentThread().getName() + ": " + this.passengerCount + " passengers disembarking...");
        atc.updatePassengersBoarded(this.passengerCount);

        this.setPassengerCount(0);

        // refilling supplies and fuel
        System.out.println(Thread.currentThread().getName() + ": Refilling Supplies...");

        atc.acquireFuelTruck();
        System.out.println(Thread.currentThread().getName() + ": Refuelling...");
        System.out.println(Thread.currentThread().getName() + ": Refuelling done");
        atc.releaseFuelTruck();

        System.out.println(Thread.currentThread().getName() + ": Receiving new passengers");
        this.setPassengerCount((int) (Math.random() * 51));

        // obtain departing permission
        this.setLanding(false);
        synchronized (this)
        {
            atc.requestDeparting(this);
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        atc.acquireRunway();
        atc.releaseGate(gateNumber);      // release gate after acquiring runway for safe departing
        System.out.println(Thread.currentThread().getName() + ": Departing on runway...");
        System.out.println(Thread.currentThread().getName() + ": Departed Successfully...");
        atc.releaseRunway();
        atc.planeFinished();
    }
}