public class MainThread
{

    public static void main(String[] args)
    {
        System.out.println("--- Asia Pacific Airport Simulation ---");

        // one ATC thread
        ATC apAtc = new ATC(3, 6);
        Thread atcThread = new Thread(apAtc, "AP_ATC");
        atcThread.start();

        // six plane threads
        for (int i = 0; i < 6; i++)
        {
            int passengers = (int) (Math.random() * 51);   // random passenger count, 0-50
            Plane plane = new Plane(i + 1, passengers, apAtc);
            Thread planeThread = new Thread(plane, "Plane-" + (i + 1));
            planeThread.start();

            try
            {
                Thread.sleep((int) (Math.random() * 2000));
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }
}
