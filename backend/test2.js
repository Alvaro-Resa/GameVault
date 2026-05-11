require('dotenv').config();
const mysql = require('mysql2');
const db = mysql.createConnection({host: process.env.DB_HOST, user: process.env.DB_USER, password: process.env.DB_PASS, database: process.env.DB_NAME});

const req = { user_id: '1', game_id: '1', status: 'playing', hours_played: '10' };

db.query('SELECT id FROM Library WHERE user_id = ?', [req.user_id], (err, libResult) => {
    console.log('LibResult:', libResult);
    
    const executeLibraryUpdate = (libraryId) => {
        db.query('SELECT * FROM games_library WHERE library_id = ? AND game_id = ?', [libraryId, req.game_id], (err, exists) => {
            console.log('Exists:', exists, 'Error:', err);
            if (exists.length === 0) {
                db.query('INSERT INTO games_library (library_id, game_id, status, hours_played) VALUES (?, ?, ?, ?)', 
                    [libraryId, req.game_id, req.status, req.hours_played], (e) => {
                    console.log('Insert Error:', e);
                    process.exit();
                });
            } else {
                console.log('Already exists');
                process.exit();
            }
        });
    };
    
    if (libResult.length === 0) {
        db.query("INSERT INTO Library (user_id, name) VALUES (?, 'My Library')", [req.user_id], (e, r) => {
            console.log('Insert Library Err:', e);
            executeLibraryUpdate(r.insertId);
        });
    } else {
        executeLibraryUpdate(libResult[0].id);
    }
});
