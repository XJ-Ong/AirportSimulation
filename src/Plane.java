public class Plane implements Runnable
{
    private int planeID;
    private int passengerCount;
    private ATC atc;
    private boolean isLanding;
    private boolean isEmergency;

    public Plane(int planeID, int passengerCount, ATC atc)
    {
        this.planeID = planeID;
        this.passengerCount = passengerCount;
        this.atc = atc;
        this.isLanding = true;
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

    @Override
    public void run()
    {

        synchronized (this)
        {
            if (getIsEmergency())
            {
                System.out.println(Thread.currentThread().getName() + ": Requesting emergency landing!");
            }
            else
            {
                System.out.println(Thread.currentThread().getName() + ": Requesting landing");
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
        }
    }
}