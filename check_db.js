const mysql = require('mysql2');
require('dotenv').config({ path: './backend/.env' });

const db = mysql.createConnection({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASS || '',
    database: process.env.DB_NAME || 'gamevault'
});

db.connect(err => {
    if (err) {
        console.error('Connection error:', err);
        process.exit(1);
    }
    db.query('SHOW TABLES', (err, tables) => {
        if (err) throw err;
        console.log('Tables:', tables);
        
        db.query('DESCRIBE Reviews', (err, desc) => {
            if (err) console.error(err);
            console.log('Reviews table:', desc);
            
            db.query('DESCRIBE mangas_library', (err, desc2) => {
                if (err) console.error(err);
                console.log('mangas_library table:', desc2);
                process.exit(0);
            });
        });
    });
});
