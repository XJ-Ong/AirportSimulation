public class Plane implements Runnable
{
    private final int planeID;
    private int passengerCount;
    private final ATC atc;
    private boolean isLanding = true;
    private boolean isEmergency;
    private int gateNumber = -1;

    // timestamps to calculate waiting time
    private long landingStart;
    private long landingEnd;
    private long departingStart;
    private long departingEnd;

    private void delay(int weightage)
    {
        int randomDelay = (int) (Math.random() * (1000 + 1)) + 1000;
        randomDelay = randomDelay * weightage;
        try
        {
            Thread.sleep(randomDelay); // if weightage = 1, sleep randomly between 0.5 to 1 second
        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Plane(int planeID, int passengerCount, ATC atc)
    {
        this.planeID = planeID;
        this.passengerCount = passengerCount;
        this.atc = atc;
        this.isEmergency = planeID == 5;
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

    public void setGateNumber(int gateNumber)
    {
        this.gateNumber = gateNumber;
    }

    public int getGateNumber()
    {
        return this.gateNumber;
    }

    @Override
    public void run()
    {
        synchronized (this)
        {
            this.landingStart = System.currentTimeMillis();
            if (getIsEmergency())
            {
                System.out.println(Thread.currentThread().getName() + ": Requesting EMERGENCY landing!");
            }
            else
            {
                System.out.println(Thread.currentThread().getName() + ": Requesting landing...");
            }

            atc.requestLanding(this);
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            this.landingEnd = System.currentTimeMillis();
        }

        // landing
        atc.acquireRunway();
        System.out.println(Thread.currentThread().getName() + ": Landing on runway...");
        this.delay(1);
        atc.releaseRunway();

        // coasting to gate
        this.delay(1);
        System.out.println(Thread.currentThread().getName() + ": Docked at Gate-" + this.getGateNumber());

        System.out.println(Thread.currentThread().getName() + ": " + this.passengerCount + " passengers disembarking...");
        this.delay(1);
        atc.updatePassengersBoarded(this.passengerCount);

        this.setPassengerCount(0);

        // refilling supplies and fuel
        System.out.println(Thread.currentThread().getName() + ": Refilling Supplies...");

        atc.acquireFuelTruck();
        System.out.println(Thread.currentThread().getName() + ": Refuelling...");
        this.delay(2);  // refuelling taking more time to simulate shared resource
        System.out.println(Thread.currentThread().getName() + ": Refuelling done");
        atc.releaseFuelTruck();

        this.setPassengerCount((int) (Math.random() * 51));
        System.out.println(Thread.currentThread().getName() + ": " + this.passengerCount + " passengers boarding...");
        this.delay(1);

        // obtain departing permission
        this.setLanding(false);
        synchronized (this)
        {
            this.departingStart = System.currentTimeMillis();
            atc.requestDeparting(this);
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            this.departingEnd = System.currentTimeMillis();
        }

        // departing
        atc.acquireRunway();
        this.delay(1);
        atc.releaseGate(this.getGateNumber());      // release gate after acquiring runway for safe departing
        System.out.println(Thread.currentThread().getName() + ": Departing on runway...");
        this.delay(1);
        System.out.println(Thread.currentThread().getName() + ": Departed Successfully...");
        atc.releaseRunway();

        // update ATC statistics
        long waitTime = (this.landingEnd - this.landingStart) + (this.departingEnd - this.departingStart);
        atc.reportWaitingTime(waitTime);
        atc.planeFinished();
    }
}