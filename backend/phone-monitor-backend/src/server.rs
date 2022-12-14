use std::{
    collections::HashMap,
    sync::{atomic::{AtomicUsize, Ordering}, Arc},
};

use actix::{Recipient, Actor, Context, Handler, Message};
use rand::{rngs::ThreadRng, Rng};

#[derive(Message)]
#[rtype(result = "()")]
pub struct SessionMessage(pub String);

pub struct ChatServer {
    // <人的id，对应的recipient>
    sessions: HashMap<usize, Recipient<SessionMessage>>,
    // 访客人数
    visitor_count: Arc<AtomicUsize>,
    rng: ThreadRng,
}

impl ChatServer {

    pub fn new(visitor_count: Arc<AtomicUsize>) -> ChatServer {
        ChatServer {
            sessions: HashMap::new(),
            visitor_count: visitor_count,
            rng: rand::thread_rng(),
        }
    }

    pub fn send_message(&self, message: &str) {
        println!("[ChatServer] [send_message] {}", message);

        for (id, rcp) in &self.sessions {
            rcp.do_send(SessionMessage(message.to_owned()))
        }

    }
 
}

impl Actor for ChatServer {
    // simple Context for communicate with other actors
    type Context = Context<Self>;
}

impl Handler<Connect> for ChatServer  {
    type Result = usize;

    fn handle(&mut self, msg: Connect, ctx: &mut Self::Context) -> Self::Result {
        println!("ChatServer # handle # Connect $ Someone joined");

        self.send_message("Someone joined");
        let id = self.rng.gen::<usize>();
        self.sessions.insert(id, msg.addr);

        let count = self.visitor_count.fetch_add(1, Ordering::SeqCst);

        self.send_message(&format!("Total visitors {count}"));

        id
    }
}

impl Handler<ClientMessage> for ChatServer {
    type Result = ();

    fn handle(&mut self, msg: ClientMessage, ctx: &mut Self::Context) -> Self::Result {
        println!("ChatServer # handle #ClientMessage");
        self.send_message(msg.msg.as_str())
    }

}

impl Handler<Disconnect> for ChatServer {
    type Result = ();

    fn handle(&mut self, msg: Disconnect, ctx: &mut Self::Context) -> Self::Result {
        println!("Someone disconnected");

        let mut old_size: usize = 0;
        if self.sessions.remove(&msg.id).is_some() {
            old_size = self.visitor_count.fetch_update(Ordering::SeqCst, Ordering::SeqCst, |x| Some(x - 1)).unwrap();
            println!("ChatServer # handle # Disconnect $ remove {}", msg.id);
        }
        self.send_message(("Disconnect $ remove ".to_string() + &msg.id.to_string() + &" ".to_string() + &(old_size - 1).to_string() + &"left".to_string()).as_str())
    }
}


#[derive(Message)]
#[rtype(result = "()")]
pub struct ClientMessage {
    pub id: usize,
    pub msg: String,
}

#[derive(Message)]
#[rtype(result = "()")]
pub struct Disconnect {
    pub id: usize,
}

#[derive(Message)]
#[rtype(usize)]
pub struct Connect {
    pub addr: Recipient<SessionMessage>,
}