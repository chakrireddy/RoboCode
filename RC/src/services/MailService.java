package services;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import robocode.battle.BattleResultsTableModel;

import com.google.appengine.api.users.User;

public class MailService {

	BattleResultsTableModel tm = null;
	public MailService(BattleResultsTableModel btm) {
		this.tm = btm;
	}
	Logger logger = Logger.getLogger(MailService.class.getName());
	public void sendMail(String sender, User receiver, String message) {

		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props);
		String msgBody = "...";
		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("s.chakravarthyreddy@gmail.com",
					"Robowars Admin"));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
					"amruth.gp@gmail.com", "Mr.Amruth"));
			StringBuffer buff = new StringBuffer();
			for (int col = 1; col < tm.getColumnCount(); col++) {
				buff.append(tm.getColumnName(col) + "\t");
			}
			for (int row = 0; row < tm.getRowCount(); row++) {
				buff.append(tm.getValueAt(row, 0) + ": ");
				for (int col = 1; col < tm.getColumnCount(); col++) {
					buff.append(tm.getValueAt(row, col) + "\t");
				}
				buff.append("\n");
			}			
			
			msg.setSubject("Score");
			msg.setText(buff.toString());
			Transport.send(msg);
		} catch (AddressException e) {
			logger.info(e.getMessage());
		} catch (MessagingException e) {
			logger.info(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			logger.info(e.getMessage());
		}
	}
}