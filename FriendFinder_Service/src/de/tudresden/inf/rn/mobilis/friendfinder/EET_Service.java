package de.tudresden.inf.rn.mobilis.friendfinder;

import java.util.ArrayList;
import java.util.logging.Logger;

import de.tudresden.inf.rn.mobilis.friendfinder.proxy.ClientLocation;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.FriendFinderProxy;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.IsTrackAvailableRequest;
import de.tudresden.inf.rn.mobilis.friendfinder.proxy.IsTrackAvailableResponse;
import de.tudresden.inf.rn.mobilis.xmpp.beans.ProxyBean;
import de.tudresden.inf.rn.mobilis.xmpp.server.BeanProviderAdapter;

public class EET_Service {
	private final static Logger LOG = Logger.getLogger(EET_Service.class
			.getCanonicalName());

	private DBProxy db;

	public EET_Service() {
		try {
			db = new DBProxy();
			db.createTables();
			db.cleanDB();
			db.printData();
			db.saveGPX();
			
		} catch (Exception e) {
			//LOG.severe("!EXCEPTION: " + e.toString() + "(" + e.getStackTrace().toString() + ")");
			e.printStackTrace();
		}
	}
	
	public void startup(){}
	
	public void shutdown(){
		if(db != null){
			db.close();
		}
	}

	public void registerPacketListener() {
		(new BeanProviderAdapter(new ProxyBean(
				IsTrackAvailableRequest.NAMESPACE,
				IsTrackAvailableRequest.CHILD_ELEMENT))).addToProviderManager();
	}

	protected IsTrackAvailableResponse isTrackAvailable(
			IsTrackAvailableRequest in) {
		IsTrackAvailableResponse out = new IsTrackAvailableResponse();
		out.setId(in.getId());
		
		if(db != null){
			ClientLocation cl = in.getPosition();
			
			if(cl.getLat() == -Double.MAX_VALUE && cl.getLng() == -Double.MAX_VALUE){
				/* rate the given track */
				db.rateTrack(in.getTrackId());
				LOG.info("Rate Track: " + in.getTrackId());
				
				return null;
			}
			else{
				ArrayList<DBProxy.Node> nodes = db.getMatchingNodes(cl.getLat(), cl.getLng(), cl.getAccuracy());
				
				if(nodes != null && nodes.size() > 0){
					/* track available for current position */
					/*LOG.info(nodes.size() + " Tracks available for Position: N" 
							+ cl.getLat() + " E" + cl.getLng() 
							+ ", choose: " + nodes.toString());*/
				
					
					// todo, welchen track auswählen?
					DBProxy.Node node = nodes.get(0);
					int trackId = node.track_fk;
					
					if(trackId == in.getTrackId()){
						//rückgabe nur der aktuelle track?, dann einfach weiter einfügen, oder überspringen
						LOG.info("Match current Track, ignore (trkid: " + Integer.toString(in.getTrackId()) + ")");
						
						out.setGpxTrack(new GPXTrack());
						out.setTrackId(trackId);
						out.setResult(false);
					}
					else{
						if(in.getTrackId() > 0){
							LOG.info("Match new Track, end (trkid: " + Integer.toString(in.getTrackId()) + ") + divide (trkid: " + Integer.toString(trackId) + ")");
							//neuer Track trifft auf bestehenden, Kreuzung, Tracks teilen
							
							db.divideTrack(trackId, node.id);
							db.cleanDB();
							
							// keinen zurückgeben, auf weitere pos warten
							out.setGpxTrack(new GPXTrack());
							out.setTrackId(0);
							out.setResult(false);
						}
						else{
							// start ortung/nach kreuzung, keine aufzeichnung, einfach rückgabe
							// todo, ab dem punkt, welche richtung?
							GPXTrack gpx = db.selectTrack(trackId);
							
							if(gpx != null){
								LOG.info("Init, return matched Track (trkid: " + Integer.toString(trackId) + ", points: " + gpx.trkpt.size() + ")");
								out.setGpxTrack(gpx);
							}	
							else out.setGpxTrack(new GPXTrack());
							
							
							out.setTrackId(trackId);
							out.setResult(true);
						}
					}
					
				} else {
					/* no track available */
					int trackId = in.getTrackId();
					if(trackId == 0){
						/* neuen Track anlegen */
						trackId = db.insertTrack(1, in.getPosition().getTime());
						LOG.info("New position received, (trkid " + Integer.toString(in.getTrackId()) + ") -> New Track " + trackId );
					}				
					else 
						LOG.info("New position received, (trkid " + Integer.toString(in.getTrackId()) + ")");
					db.insertNode(in.getPosition(), trackId);
					out.setGpxTrack(new GPXTrack());
					out.setTrackId(trackId);
					out.setResult(false);
					
					
				}
			}
			//db.printData();
			db.saveGPX();
		}
		
		return out;
	}

	public Boolean processPacket(ProxyBean proxyBean, FriendFinderProxy _proxy) {
		if (proxyBean.isTypeOf(IsTrackAvailableRequest.NAMESPACE,
				IsTrackAvailableRequest.CHILD_ELEMENT)) {

			IsTrackAvailableResponse response = isTrackAvailable((IsTrackAvailableRequest) proxyBean
					.parsePayload(new IsTrackAvailableRequest()));
			
			if(response != null){
				_proxy.IsTrackAvailable(proxyBean.getFrom(), response.getId(),
						response.getResult(), response.getTrackId(), response.getGpxTrack());
			}
			return true;

		} else
			return false;
	}
}
