package org.esoul.surpass.gui.event;

@FunctionalInterface
public interface AddUpdateSecretListener {

   void actionPerformed(char[] secret, char[] identifier, char[] note) throws Exception;
}
