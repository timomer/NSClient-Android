NS Client
===========

 - What it does?
 
 It just connects to your Nightscout server and provide support for other applications.
 
 - Does it have GUI?
 
 No GUI suitable for regular users at the moment. But maybe someone write it.
 
 - How to use it?
 
 Write your own app and listen to broadcasts to get new data from Nightscout. Send broadcasts to write to mongo database.
 
 - Any other features there?
 
 NS Client can emulate xDrip app if you don't have physical device available. It supports xDrip extension for DanaApp too (OpenAPS implementation for DanaR pump)
 
 - Some examples?
 
 Look at the and of MainActivity
 
 - And how do I get response?
 
 When you upload treatment you should receieve it back if you listen to treatment broadcasts after it goes through mongo
 
 - Do I need care about success/failure of upload?
 
 No. NS client takas care about it. Undelivered records are queued and delivered when client reconnects to server.
  
 - Are there any limitations?
 
 When record is stored to queue, there is not preserved upload order on reconnection. When you send `dbUpdate` or `dbUpdateUnset` only last request 
 to the same record is stored to queue.