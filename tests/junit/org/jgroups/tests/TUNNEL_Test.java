package org.jgroups.tests;


import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.protocols.MERGE2;
import org.jgroups.stack.GossipRouter;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Promise;
import org.jgroups.util.Util;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Ensures that a disconnected channel reconnects correctly, for different
 * stack configurations.
 *
 * @author Ovidiu Feodorov <ovidiu@feodorov.com>
 * @author Bela Ban belaban@yahoo.com
 * @version $Id: TUNNEL_Test.java,v 1.12 2009/10/01 15:51:07 vlada Exp $
 **/
@Test(groups={Global.STACK_INDEPENDENT, Global.GOSSIP_ROUTER},sequential=true)
public class TUNNEL_Test extends ChannelTestBase{
    private JChannel channel, coordinator;
    private final static String GROUP="TUNNEL_Test";
    private GossipRouter gossipRouter;
    private static final String props="tunnel.xml";

    @BeforeClass
    void startRouter() throws Exception {
        gossipRouter=new GossipRouter();
        gossipRouter.start();
    }
    
    @AfterClass
    void stopRouter() throws Exception {
        gossipRouter.stop();
    }

    @AfterMethod
    void tearDown() throws Exception {
        Util.close(channel, coordinator);
    }




    /**
     * Tests if the channel has a null local address after disconnect (using TUNNEL).
     **/
    public void testNullLocalAddress_TUNNEL() throws Exception {
        channel = new JChannel(props);
        setProps(channel);
        channel.connect(GROUP);
        assert channel.getAddress() != null;
        channel.disconnect();
        assert channel.getAddress() == null;
    }


    /**
     * Tests connect-disconnect-connect sequence for a group with one member
     * (using default configuration).
     **/
    public void testDisconnectConnectOne_Default() throws Exception {
        channel=new JChannel(props);
        setProps(channel);
        channel.connect("DisconnectTest.testgroup-1");
        channel.disconnect();
        channel.connect("DisconnectTest.testgroup-2");
        View view=channel.getView();
        assert view.size() == 1;
        assert view.containsMember(channel.getAddress());
    }


    /**
     * Tests connect-disconnect-connect sequence for a group with two members
     * (using default configuration).
     **/
    public void testDisconnectConnectTwo_Default() throws Exception {
        coordinator=new JChannel(props);
        setProps(coordinator);

        channel=new JChannel(props);
        setProps(channel);
        
        coordinator.connect(GROUP);
        channel.connect("DisconnectTest.testgroup-1");
        channel.disconnect();
        channel.connect(GROUP);
        View view=channel.getView();
        assert view.size() == 2;
        assert view.containsMember(channel.getAddress());
        assert view.containsMember(coordinator.getAddress());
    }


    /**
     * Tests connect-disconnect-connect-send sequence for a group with two
     * members, using the default stack configuration. Assumes that default
     * configuration includes pbcast.NAKACK. Test case introduced before fixing
     * pbcast.NAKACK bug, which used to leave pbcast.NAKACK in a broken state
     * after DISCONNECT. Because of this problem, the channel couldn't be used
     * to multicast messages.
     **/
    public void testDisconnectConnectSendTwo_Default() throws Exception {
        final Promise<Message> msgPromise=new Promise<Message>();
        coordinator=new JChannel(props);
        setProps(coordinator);
        coordinator.connect(GROUP);
        coordinator.setReceiver(new PromisedMessageListener(msgPromise));

        channel=new JChannel(props);
        setProps(channel);
        channel.connect("DisconnectTest.testgroup-1");
        channel.disconnect();
        channel.connect(GROUP);

        channel.send(new Message(null, null, "payload"));

        Message msg=(Message)msgPromise.getResult(20000);
        assert msg != null;
        assert "payload".equals(msg.getObject());
    }


    /**
      * Tests connect-disconnect-connect sequence for a group with one member
      * (using TUNNEL).
      **/
     public void testDisconnectConnectOne_TUNNEL() throws Exception {
        channel=new JChannel(props);
        setProps(channel);
        channel.connect("DisconnectTest.testgroup-1");
        channel.disconnect();
        channel.connect("DisconnectTest.testgroup-2");
        View view=channel.getView();
        assert view.size() == 1;
        assert view.containsMember(channel.getAddress());
    }
     
     public void testFailureDetection() throws Exception {
         coordinator=new JChannel(props);
         setProps(coordinator);
         coordinator.connect(GROUP);
         
         channel=new JChannel(props);
         setProps(channel);       
         channel.connect(GROUP);
         
         channel.shutdown();
         long now = System.currentTimeMillis();
         //amount of sleep is more than VERIFY_SUSPECT but less than timeout in FD
         Util.sleep(3000);
         assert coordinator.getView().size() ==1;
     }
     
     public void testConnectThree() throws Exception {
         coordinator=new JChannel(props);
         setProps(coordinator);

         channel=new JChannel(props);
         setProps(channel);
         
         coordinator.connect(GROUP);
         channel.connect(GROUP);
         
         JChannel third = new JChannel (props);
         third.connect(GROUP);
         
         View view=channel.getView();
         assert channel.getView().size() == 3;
         assert third.getView().size() == 3;
         assert view.containsMember(channel.getAddress());
         assert view.containsMember(coordinator.getAddress());
         
         Util.close(third);
     }


     /**
      * Tests connect-disconnect-connect sequence for a group with two members
      * (using TUNNEL).
      **/
     public void testDisconnectConnectTwo_TUNNEL() throws Exception {
         coordinator=new JChannel(props);
         setProps(coordinator);
         coordinator.connect(GROUP);
         channel=new JChannel(props);
         setProps(channel);
         channel.connect("DisconnectTest.testgroup-1");
         channel.disconnect();
         channel.connect(GROUP);

         Thread.sleep(1000);

         View view=channel.getView();
         assert view.size() == 2;
         assert view.containsMember(channel.getAddress());
         assert view.containsMember(coordinator.getAddress());
     }


    /**
     * Tests connect-disconnect-connect-send sequence for a group with two
      * members, using TUNNEL. Test case introduced before fixing pbcast.NAKACK
      * bug, which used to leave pbcast.NAKACK in a broken state after
      * DISCONNECT. Because of this problem, the channel couldn't be used to
      * multicast messages.
      **/
     public void testDisconnectConnectSendTwo_TUNNEL() throws Exception {
        final Promise<Message> msgPromise=new Promise<Message>();
        coordinator=new JChannel(props);
        setProps(coordinator);
        coordinator.connect(GROUP);
        coordinator.setReceiver(new PromisedMessageListener(msgPromise));

        channel=new JChannel(props);
        setProps(channel);
        channel.connect("DisconnectTest.testgroup-1");
        channel.disconnect();
        channel.connect(GROUP);

        channel.send(new Message(null, null, "payload"));

        Message msg=msgPromise.getResult(20000);
        assert msg != null;
        assert "payload".equals(msg.getObject());
    }

    private static void setProps(JChannel channel) {
        ProtocolStack stack=channel.getProtocolStack();
        MERGE2 merge=(MERGE2)stack.findProtocol(MERGE2.class);
        if(merge != null) {
            merge.setMinInterval(1000);
            merge.setMaxInterval(3000);
        }
    }



    private static class PromisedMessageListener extends ReceiverAdapter {
        private final Promise<Message> promise;

        public PromisedMessageListener(Promise<Message> promise) {
            this.promise=promise;
        }

        public void receive(Message msg) {
            promise.setResult(msg);
        }
    }

}